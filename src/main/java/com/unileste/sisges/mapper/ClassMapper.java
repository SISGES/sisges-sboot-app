package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.controller.dto.response.DetailedClassResponseDto;
import com.unileste.sisges.model.ClassEntity;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class ClassMapper {

    public static ClassEntity toEntity(CreateClassRequestDto entity) {
        return ClassEntity
                .builder()
                .name(entity.getName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ClassResponseDto toResponse(ClassEntity entity) {
        return ClassResponseDto
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static DetailedClassResponseDto toDetailedResponse(ClassEntity entity) {
        return DetailedClassResponseDto
                .builder()
                .name(entity.getName())
                .students(entity.getStudents().stream().map(StudentMapper::toStudentResponseDto).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}