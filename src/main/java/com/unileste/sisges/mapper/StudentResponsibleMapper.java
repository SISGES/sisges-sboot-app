package com.unileste.sisges.mapper;

import com.unileste.sisges.controller.dto.request.StudentResponsibleRequest;
import com.unileste.sisges.controller.dto.response.StudentResponsibleResponse;
import com.unileste.sisges.model.StudentResponsible;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StudentResponsibleMapper {
    public static StudentResponsible toEntity(StudentResponsibleRequest request) {
        return StudentResponsible
                .builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .alternativeEmail(request.getAlternativeEmail())
                .alternativePhone(request.getAlternativePhone())
                .build();
    }

    public static StudentResponsibleResponse toResponse(StudentResponsible entity) {
        return StudentResponsibleResponse
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .alternativePhone(entity.getAlternativePhone())
                .email(entity.getAlternativeEmail())
                .build();
    }
}
