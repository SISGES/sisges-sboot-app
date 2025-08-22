package com.unileste.sisges.controller.dto.response;

import com.unileste.sisges.enums.converter.GenderENUM;
import com.unileste.sisges.model.ClassEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
public class StudentResponseDto {

    private String register;
    private String name;
    private String email;
    private ClassResponseDto classEntity;
    private LocalDate birthDate;
    private GenderENUM gender;
}