package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
public class StudentResponse {

    private String register;
    private String name;
    private String email;
    private ClassResponse classEntity;
    private LocalDate birthDate;
    private String gender;
}