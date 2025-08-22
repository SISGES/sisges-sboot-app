package com.unileste.sisges.controller.dto.request;

import com.unileste.sisges.enums.converter.GenderENUM;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateStudentDto {

    @NotBlank
    private String name;
    @NotBlank
    private String email;
    @NotBlank
    private String responsible1Name;
    @NotBlank
    private String responsible1Phone;
    @NotBlank
    private String responsible1Email;
    private String responsible2Name;
    private String responsible2Phone;
    private String responsible2Email;
    @NotNull
    private LocalDate birthDate;
    @NotNull
    private GenderENUM gender;
    @NotNull
    private Integer classId;
}