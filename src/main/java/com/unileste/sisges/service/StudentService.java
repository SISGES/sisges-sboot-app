package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.schoolclass.StudentSimpleResponse;
import com.unileste.sisges.controller.dto.schoolclass.TeacherSimpleResponse;
import com.unileste.sisges.controller.dto.student.AttendanceResponse;
import com.unileste.sisges.controller.dto.student.ClassMeetingResponse;
import com.unileste.sisges.controller.dto.student.DisciplineAbsenceCountResponse;
import com.unileste.sisges.controller.dto.student.MyClassResponse;
import com.unileste.sisges.controller.dto.student.ResponsibleResponse;
import com.unileste.sisges.controller.dto.student.SchoolClassSimpleResponse;
import com.unileste.sisges.controller.dto.student.StudentResponse;
import com.unileste.sisges.controller.dto.student.StudentSearchRequest;
import com.unileste.sisges.controller.dto.student.StudentSearchResponse;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Attendance;
import com.unileste.sisges.model.ClassMeeting;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.StudentResponsible;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.specification.StudentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<StudentSearchResponse> searchStudents(StudentSearchRequest request) {
        Specification<Student> spec = StudentSpecification.withFilters(request);
        return studentRepository.findAll(spec)
                .stream()
                .map(this::toStudentSearchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Integer id) {
        Student student = studentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", id));
        return toStudentResponse(student);
    }

    @Transactional(readOnly = true)
    public MyClassResponse getMyClassForStudentUser(Integer userId) {
        Student student = studentRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", userId));
        SchoolClass sc = student.getCurrentClass();
        if (sc == null || sc.getDeletedAt() != null) {
            return MyClassResponse.builder()
                    .className(null)
                    .academicYear(null)
                    .classmates(List.of())
                    .teachers(List.of())
                    .build();
        }
        List<StudentSimpleResponse> classmates = studentRepository.findByCurrentClass_IdAndDeletedAtIsNull(sc.getId())
                .stream()
                .filter(p -> !p.getId().equals(student.getId()))
                .filter(p -> p.getBaseData() != null && p.getDeletedAt() == null)
                .map(p -> StudentSimpleResponse.builder()
                        .id(p.getId())
                        .name(p.getBaseData().getName())
                        .email(p.getBaseData().getEmail())
                        .build())
                .sorted(Comparator.comparing(StudentSimpleResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<TeacherSimpleResponse> teachers = sc.getTeachers()
                .stream()
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> t.getBaseData() != null)
                .map(t -> TeacherSimpleResponse.builder()
                        .id(t.getId())
                        .name(t.getBaseData().getName())
                        .email(t.getBaseData().getEmail())
                        .build())
                .sorted(Comparator.comparing(TeacherSimpleResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return MyClassResponse.builder()
                .className(sc.getName())
                .academicYear(sc.getAcademicYear())
                .classmates(classmates)
                .teachers(teachers)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DisciplineAbsenceCountResponse> getMyAbsenceCountsByDiscipline(Integer userId) {
        Student student = studentRepository.findByBaseData_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", userId));
        SchoolClass sc = student.getCurrentClass();
        if (sc == null || sc.getDeletedAt() != null) {
            return List.of();
        }
        Map<Integer, Long> absenceByDisciplineId = student.getAttendances()
                .stream()
                .filter(a -> a.getDeletedAt() == null)
                .filter(a -> Boolean.FALSE.equals(a.getPresent()))
                .filter(a -> {
                    ClassMeeting cm = a.getClassMeeting();
                    return cm != null && cm.getDeletedAt() == null && cm.getDiscipline() != null;
                })
                .collect(Collectors.groupingBy(
                        a -> a.getClassMeeting().getDiscipline().getId(),
                        Collectors.counting()));
        return sc.getDisciplines()
                .stream()
                .filter(d -> d.getDeletedAt() == null)
                .sorted(Comparator.comparing(Discipline::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(d -> DisciplineAbsenceCountResponse.builder()
                        .disciplineName(d.getName())
                        .absenceCount(absenceByDisciplineId.getOrDefault(d.getId(), 0L).intValue())
                        .build())
                .toList();
    }

    private StudentSearchResponse toStudentSearchResponse(Student student) {
        User baseData = student.getBaseData();
        return StudentSearchResponse.builder()
                .id(student.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .build();
    }

    private StudentResponse toStudentResponse(Student student) {
        User baseData = student.getBaseData();

        SchoolClassSimpleResponse currentClassResponse = null;
        SchoolClass currentClass = student.getCurrentClass();
        if (currentClass != null && currentClass.getDeletedAt() == null) {
            currentClassResponse = SchoolClassSimpleResponse.builder()
                    .id(currentClass.getId())
                    .name(currentClass.getName())
                    .academicYear(currentClass.getAcademicYear())
                    .build();
        }

        List<AttendanceResponse> attendancesResponse = student.getAttendances()
                .stream()
                .filter(a -> a.getDeletedAt() == null)
                .map(this::toAttendanceResponse)
                .toList();

        List<ResponsibleResponse> responsiblesResponse = student.getResponsibles()
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(this::toResponsibleResponse)
                .toList();

        return StudentResponse.builder()
                .id(student.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .register(baseData.getRegister())
                .birthDate(baseData.getBirthDate())
                .gender(baseData.getGender())
                .currentClass(currentClassResponse)
                .attendances(attendancesResponse)
                .responsibles(responsiblesResponse)
                .build();
    }

    private AttendanceResponse toAttendanceResponse(Attendance attendance) {
        ClassMeeting meeting = attendance.getClassMeeting();
        
        ClassMeetingResponse meetingResponse = ClassMeetingResponse.builder()
                .disciplineName(meeting.getDiscipline().getName())
                .meetingDate(meeting.getMeetingDate())
                .createdAt(meeting.getCreatedAt())
                .build();

        return AttendanceResponse.builder()
                .classMeeting(meetingResponse)
                .present(attendance.getPresent())
                .build();
    }

    private ResponsibleResponse toResponsibleResponse(StudentResponsible responsible) {
        return ResponsibleResponse.builder()
                .id(responsible.getId())
                .name(responsible.getName())
                .phone(responsible.getPhone())
                .email(responsible.getEmail())
                .build();
    }
}
