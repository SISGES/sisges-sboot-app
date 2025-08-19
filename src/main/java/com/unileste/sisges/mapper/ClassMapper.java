package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.CreateClassRequestDto;
import com.unileste.sisges.controller.dto.response.ClassResponseDto;
import com.unileste.sisges.model.ClassEntity;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class ClassMapper {

    public static ClassEntity toEntity(CreateClassRequestDto entity){
        return ClassEntity
                .builder()
                .name(entity.getName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ClassResponseDto toResponse(ClassEntity entity){
        return ClassResponseDto
                .builder()
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}