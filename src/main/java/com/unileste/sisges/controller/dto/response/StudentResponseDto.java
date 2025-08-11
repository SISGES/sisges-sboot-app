package com.unileste.sisges.controller.dto.response;

import com.unileste.sisges.enums.converter.GenderENUM;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Builder
public class StudentResponseDto {

    private String register;
    private String name;
    private LocalDate birthDate;
    private GenderENUM gender;
}