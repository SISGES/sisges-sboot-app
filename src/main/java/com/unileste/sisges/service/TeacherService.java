package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.teacher.SchoolClassSimpleResponse;
import com.unileste.sisges.controller.dto.teacher.TeacherResponse;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchRequest;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.TeacherRepository;
import com.unileste.sisges.repository.specification.TeacherSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<TeacherSearchResponse> searchTeachers(TeacherSearchRequest request) {
        Specification<Teacher> spec = TeacherSpecification.withFilters(request);
        return teacherRepository.findAll(spec)
                .stream()
                .map(this::toTeacherSearchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Integer id) {
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor", id));
        return toTeacherResponse(teacher);
    }

    private TeacherSearchResponse toTeacherSearchResponse(Teacher teacher) {
        User baseData = teacher.getBaseData();
        return TeacherSearchResponse.builder()
                .id(teacher.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .build();
    }

    private TeacherResponse toTeacherResponse(Teacher teacher) {
        User baseData = teacher.getBaseData();
        
        List<SchoolClassSimpleResponse> classes = teacher.getClasses()
                .stream()
                .filter(sc -> sc.getDeletedAt() == null)
                .map(sc -> SchoolClassSimpleResponse.builder()
                        .id(sc.getId())
                        .name(sc.getName())
                        .academicYear(sc.getAcademicYear())
                        .build())
                .toList();

        return TeacherResponse.builder()
                .id(teacher.getId())
                .name(baseData.getName())
                .email(baseData.getEmail())
                .register(baseData.getRegister())
                .birthDate(baseData.getBirthDate())
                .gender(baseData.getGender())
                .classes(classes)
                .build();
    }
}
