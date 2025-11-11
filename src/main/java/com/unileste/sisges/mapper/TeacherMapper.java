package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.response.TeacherResponse;
import com.unileste.sisges.model.Teacher;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeacherMapper {

    public static TeacherResponse toResponse(Teacher entity) {
        return TeacherResponse
                .builder()
                .classes(entity.getClasses() != null ? entity.getClasses().stream().map(ClassMapper::toResponse).toList() : null)
                .build();
    }
}