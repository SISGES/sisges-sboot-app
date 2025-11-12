package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.model.Student;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StudentMapper {

    public static StudentResponse toResponse(Student entity) {
        return StudentResponse
                .builder()
                .id(entity.getId())
                .userId(entity.getBaseData().getId())
                .currentClass(entity.getCurrentClass() != null ? entity.getCurrentClass().getId() : null)
                .name(entity.getBaseData().getName())
                .email(entity.getBaseData().getEmail())
                .build();
    }
}