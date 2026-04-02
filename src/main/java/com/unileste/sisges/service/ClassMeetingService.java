package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.classmeeting.*;
import com.unileste.sisges.controller.dto.schoolclass.StudentSimpleResponse;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.*;
import com.unileste.sisges.repository.specification.ClassMeetingSpecification;
import com.unileste.sisges.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassMeetingService {

    private final ClassMeetingRepository classMeetingRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final DisciplineRepository disciplineRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public ClassMeetingSearchResponse create(CreateClassMeetingRequest request, Integer teacherUserId) {
        Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(teacherUserId)
                .orElseThrow(() -> new BusinessRuleException("Apenas professores podem lançar aulas."));

        ClassMeeting meeting = validateAndCreate(request, teacher, null);
        return toSearchResponse(meeting);
    }

    private void validateMeetingSchedule(CreateClassMeetingRequest request, Teacher teacher, Integer excludeMeetingId) {
        SchoolClass schoolClass = schoolClassRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma", request.getClassId()));

        Discipline discipline = disciplineRepository.findByIdAndDeletedAtIsNull(request.getDisciplineId())
                .orElseThrow(() -> new BusinessRuleException("Disciplina inexistente."));

        if (!schoolClass.getDisciplines().stream().anyMatch(d -> d.getId().equals(discipline.getId()))) {
            throw new BusinessRuleException("A disciplina não está vinculada à turma.");
        }
        if (!schoolClass.getTeachers().stream().anyMatch(t -> t.getId().equals(teacher.getId()))) {
            throw new BusinessRuleException("O professor não está vinculado à turma.");
        }

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessRuleException("Horário de início deve ser anterior ao horário de término.");
        }

        List<ClassMeeting> overlapping = classMeetingRepository.findOverlappingMeetings(
                request.getClassId(), request.getDate(), request.getStartTime(), request.getEndTime());
        if (excludeMeetingId != null) {
            overlapping = overlapping.stream().filter(cm -> !cm.getId().equals(excludeMeetingId)).toList();
        }
        if (!overlapping.isEmpty()) {
            throw new BusinessRuleException("Uma turma não pode ter mais de uma aula ao mesmo tempo. Já existe aula cadastrada com horário sobreposto.");
        }
    }

    private ClassMeeting validateAndCreate(CreateClassMeetingRequest request, Teacher teacher, Integer excludeMeetingId) {
        validateMeetingSchedule(request, teacher, excludeMeetingId);

        SchoolClass schoolClass = schoolClassRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma", request.getClassId()));
        Discipline discipline = disciplineRepository.findByIdAndDeletedAtIsNull(request.getDisciplineId())
                .orElseThrow(() -> new BusinessRuleException("Disciplina inexistente."));

        ClassMeeting meeting = ClassMeeting.builder()
                .schoolClass(schoolClass)
                .discipline(discipline)
                .teacher(teacher)
                .meetingDate(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        return classMeetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public List<ClassMeetingSearchResponse> search(ClassMeetingSearchRequest request, UserPrincipal principal) {
        Specification<ClassMeeting> spec;
        if (principal != null && "TEACHER".equals(principal.getRole())) {
            Teacher teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Professor não encontrado."));
            spec = ClassMeetingSpecification.withFiltersAndTeacher(request, teacher.getId());
        } else {
            spec = ClassMeetingSpecification.withFilters(request);
        }
        return classMeetingRepository.findAll(spec).stream()
                .map(this::toSearchResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassMeetingDetailResponse findById(Integer id, UserPrincipal principal) {
        ClassMeeting meeting = classMeetingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aula", id));

        if (principal != null && "TEACHER".equals(principal.getRole())) {
            Teacher current = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Professor não encontrado."));
            Teacher assigned = meeting.getTeacher();
            if (assigned == null || assigned.getDeletedAt() != null
                    || !assigned.getId().equals(current.getId())) {
                throw new AccessDeniedException("Acesso negado: esta aula não está vinculada a você.");
            }
        }

        SchoolClass sc = meeting.getSchoolClass();
        var attendanceMap = meeting.getAttendances().stream()
                .filter(a -> a.getDeletedAt() == null)
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Attendance::getPresent, (a, b) -> a));
        List<StudentSimpleResponse> students = sc.getStudents().stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(s -> StudentSimpleResponse.builder()
                        .id(s.getId())
                        .name(s.getBaseData().getName())
                        .email(s.getBaseData().getEmail())
                        .present(attendanceMap.get(s.getId()))
                        .build())
                .collect(Collectors.toList());

        ClassMeetingDetailResponse.ClassInfo classInfo = ClassMeetingDetailResponse.ClassInfo.builder()
                .id(sc.getId())
                .name(sc.getName())
                .academicYear(sc.getAcademicYear())
                .students(students)
                .build();

        TeacherSearchResponse teacherResponse = null;
        if (meeting.getTeacher() != null && meeting.getTeacher().getDeletedAt() == null) {
            Teacher t = meeting.getTeacher();
            teacherResponse = TeacherSearchResponse.builder()
                    .id(t.getId())
                    .name(t.getBaseData().getName())
                    .email(t.getBaseData().getEmail())
                    .build();
        }

        return ClassMeetingDetailResponse.builder()
                .id(meeting.getId())
                .date(meeting.getMeetingDate())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .classInfo(classInfo)
                .teacher(teacherResponse)
                .build();
    }

    @Transactional
    public void delete(Integer id) {
        ClassMeeting meeting = classMeetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aula", id));
        meeting.setDeletedAt(LocalDateTime.now());
        classMeetingRepository.save(meeting);
    }

    @Transactional
    public ClassMeetingDetailResponse update(Integer id, UpdateClassMeetingRequest request, UserPrincipal principal) {
        ClassMeeting meeting = classMeetingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aula", id));

        Teacher teacher;
        if (request.getTeacherId() != null) {
            teacher = teacherRepository.findByIdAndDeletedAtIsNull(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professor", request.getTeacherId()));
        } else if ("ADMIN".equals(principal.getRole())) {
            teacher = meeting.getTeacher();
            if (teacher == null) {
                throw new BusinessRuleException("Aula sem professor vinculado. Informe teacherId ou associe um professor antes de editar.");
            }
        } else {
            teacher = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(principal.getId())
                    .orElseThrow(() -> new BusinessRuleException("Apenas professores podem editar aulas."));
        }

        CreateClassMeetingRequest createReq = CreateClassMeetingRequest.builder()
                .date(request.getDate())
                .disciplineId(request.getDisciplineId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .classId(request.getClassId())
                .build();

        validateMeetingSchedule(createReq, teacher, id);

        meeting.setMeetingDate(request.getDate());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setSchoolClass(schoolClassRepository.findByIdAndDeletedAtIsNull(request.getClassId()).orElseThrow());
        meeting.setDiscipline(disciplineRepository.findByIdAndDeletedAtIsNull(request.getDisciplineId()).orElseThrow());
        meeting.setTeacher(teacher);
        classMeetingRepository.save(meeting);

        return findById(id, principal);
    }

    @Transactional
    public void saveFrequency(Integer meetingId, FrequencyRequest request, UserPrincipal principal) {
        ClassMeeting meeting = classMeetingRepository.findByIdAndDeletedAtIsNull(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula", meetingId));

        if (principal != null && "TEACHER".equals(principal.getRole())) {
            Teacher current = teacherRepository.findByBaseData_IdAndDeletedAtIsNull(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Professor não encontrado."));
            Teacher assigned = meeting.getTeacher();
            if (assigned == null || assigned.getDeletedAt() != null
                    || !assigned.getId().equals(current.getId())) {
                throw new AccessDeniedException("Acesso negado: esta aula não está vinculada a você.");
            }
        }

        List<Student> classStudents = meeting.getSchoolClass().getStudents().stream()
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        for (FrequencyEntryRequest entry : request.getEntries()) {
            Student student = studentRepository.findByIdAndDeletedAtIsNull(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aluno", entry.getStudentId()));

            if (classStudents.stream().noneMatch(s -> s.getId().equals(student.getId()))) {
                throw new BusinessRuleException("Aluno " + student.getBaseData().getName() + " não pertence à turma desta aula.");
            }

            boolean present = "P".equals(entry.getStatus());

            attendanceRepository.findByClassMeeting_IdAndStudent_Id(meetingId, student.getId())
                    .ifPresentOrElse(
                            att -> {
                                att.setPresent(present);
                                attendanceRepository.save(att);
                            },
                            () -> {
                                Attendance att = Attendance.builder()
                                        .classMeeting(meeting)
                                        .student(student)
                                        .present(present)
                                        .build();
                                attendanceRepository.save(att);
                            }
                    );
        }
    }

    private ClassMeetingSearchResponse toSearchResponse(ClassMeeting cm) {
        String teacherName = cm.getTeacher() != null && cm.getTeacher().getDeletedAt() == null
                ? cm.getTeacher().getBaseData().getName()
                : null;
        return ClassMeetingSearchResponse.builder()
                .id(cm.getId())
                .date(cm.getMeetingDate())
                .startTime(cm.getStartTime())
                .endTime(cm.getEndTime())
                .disciplineName(cm.getDiscipline().getName())
                .className(cm.getSchoolClass().getName())
                .teacherName(teacherName)
                .build();
    }
}
