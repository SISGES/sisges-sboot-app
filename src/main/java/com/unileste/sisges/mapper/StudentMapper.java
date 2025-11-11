package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.model.Student;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StudentMapper {

    public static StudentResponse toResponse(Student request) {
        return StudentResponse
                .builder()
                .classEntity(request.getCurrentClass() != null ? ClassMapper.toResponse(request.getCurrentClass()) : null)
                .build();
    }
}