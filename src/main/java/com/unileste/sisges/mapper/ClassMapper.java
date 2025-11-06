package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponse;
import com.unileste.sisges.controller.dto.response.DetailedClassResponse;
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

    public static ClassResponse toResponse(SchoolClass entity) {
        return ClassResponse
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static DetailedClassResponse toDetailedResponse(SchoolClass entity) {
        return DetailedClassResponse
                .builder()
                .name(entity.getName())
                .students(entity.getStudents().stream().map(StudentMapper::toResponse).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}