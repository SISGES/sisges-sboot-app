package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.SchoolClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedSchoolClassResponse;
import com.unileste.sisges.model.SchoolClass;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class ClassMapper {

    public static SchoolClass toEntity(CreateClassRequestDto entity) {
        return SchoolClass
                .builder()
                .name(entity.getName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static SchoolClassResponse toResponse(SchoolClass entity) {
        return SchoolClassResponse
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .studentCount(entity.getStudents().size())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static DetailedSchoolClassResponse toDetailedResponse(SchoolClass entity) {
        return DetailedSchoolClassResponse
                .builder()
                .name(entity.getName())
                .students(entity.getStudents().stream().map(StudentMapper::toResponse).toList())
                .teachers(entity.getTeachers().stream().map(TeacherMapper::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}