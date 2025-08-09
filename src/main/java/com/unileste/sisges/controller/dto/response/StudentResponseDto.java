package com.unileste.sisges.controller.dto.response;

import com.unileste.sisges.enums.converter.GenderENUM;

import java.time.LocalDate;

public class StudentResponseDto {

    private String register;
    private String name;
    private LocalDate birthDate;
    private GenderENUM gender;

}