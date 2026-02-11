package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.schoolclass.CreateSchoolClassRequest;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassResponse;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassSearchRequest;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassSearchResponse;
import com.unileste.sisges.controller.dto.schoolclass.StudentSimpleResponse;
import com.unileste.sisges.controller.dto.schoolclass.TeacherSimpleResponse;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.DisciplineRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.repository.specification.SchoolClassSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final DisciplineRepository disciplineRepository;

    @Transactional
    public SchoolClassResponse create(CreateSchoolClassRequest request) {
        SchoolClass schoolClass = SchoolClass.builder()
                .name(request.getName())
                .academicYear(request.getAcademicYear())
                .students(new ArrayList<>())
                .teachers(new ArrayList<>())
                .disciplines(new ArrayList<>())
                .build();

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            List<Student> students = request.getStudentIds().stream()
                    .map(id -> studentRepository.findByIdAndDeletedAtIsNull(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Aluno", id)))
                    .toList();
            
            for (Student student : students) {
                student.setCurrentClass(schoolClass);
                schoolClass.getStudents().add(student);
            }
        }

        if (request.getTeacherIds() != null && !request.getTeacherIds().isEmpty()) {
            List<Teacher> teachers = request.getTeacherIds().stream()
                    .map(id -> teacherRepository.findByIdAndDeletedAtIsNull(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Professor", id)))
                    .toList();
            schoolClass.getTeachers().addAll(teachers);
        }

        if (request.getDisciplineIds() != null && !request.getDisciplineIds().isEmpty()) {
            List<Discipline> disciplines = request.getDisciplineIds().stream()
                    .map(id -> disciplineRepository.findByIdAndDeletedAtIsNull(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Disciplina", id)))
                    .toList();
            schoolClass.getDisciplines().addAll(disciplines);
        }

        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return toSchoolClassResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SchoolClassSearchResponse> search(SchoolClassSearchRequest request) {
        Specification<SchoolClass> spec = SchoolClassSpecification.withFilters(request);
        return schoolClassRepository.findAll(spec)
                .stream()
                .map(this::toSchoolClassSearchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse findById(Integer id) {
        SchoolClass schoolClass = schoolClassRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turma", id));
        return toSchoolClassResponse(schoolClass);
    }

    private SchoolClassSearchResponse toSchoolClassSearchResponse(SchoolClass schoolClass) {
        int studentCount = (int) schoolClass.getStudents().stream()
                .filter(s -> s.getDeletedAt() == null)
                .count();
        
        int teacherCount = (int) schoolClass.getTeachers().stream()
                .filter(t -> t.getDeletedAt() == null)
                .count();

        return SchoolClassSearchResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .academicYear(schoolClass.getAcademicYear())
                .studentCount(studentCount)
                .teacherCount(teacherCount)
                .build();
    }

    private SchoolClassResponse toSchoolClassResponse(SchoolClass schoolClass) {
        List<StudentSimpleResponse> students = schoolClass.getStudents().stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(this::toStudentSimpleResponse)
                .toList();

        List<TeacherSimpleResponse> teachers = schoolClass.getTeachers().stream()
                .filter(t -> t.getDeletedAt() == null)
                .map(this::toTeacherSimpleResponse)
                .toList();

        return SchoolClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .academicYear(schoolClass.getAcademicYear())
                .students(students)
                .teachers(teachers)
                .build();
    }

    private StudentSimpleResponse toStudentSimpleResponse(Student student) {
        User baseData = student.getBaseData();
        return StudentSimpleResponse.builder()
                .id(student.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .build();
    }

    private TeacherSimpleResponse toTeacherSimpleResponse(Teacher teacher) {
        User baseData = teacher.getBaseData();
        return TeacherSimpleResponse.builder()
                .id(teacher.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .build();
    }
}