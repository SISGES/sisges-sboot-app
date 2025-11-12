package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.model.Student;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StudentMapper {

    public static StudentResponse toResponse(Student entity) {
        return StudentResponse
                .builder()
                .name(entity.getBaseData().getName())
                .email(entity.getBaseData().getEmail())
                .classEntity(entity.getCurrentClass() != null ? ClassMapper.toResponse(entity.getCurrentClass()) : null)
                .build();
    }
}