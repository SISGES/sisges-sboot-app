package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.student.AttendanceResponse;
import com.unileste.sisges.controller.dto.student.ClassMeetingResponse;
import com.unileste.sisges.controller.dto.student.ResponsibleResponse;
import com.unileste.sisges.controller.dto.student.SchoolClassSimpleResponse;
import com.unileste.sisges.controller.dto.student.StudentResponse;
import com.unileste.sisges.controller.dto.student.StudentSearchRequest;
import com.unileste.sisges.controller.dto.student.StudentSearchResponse;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Attendance;
import com.unileste.sisges.model.ClassMeeting;
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

import java.util.List;

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
