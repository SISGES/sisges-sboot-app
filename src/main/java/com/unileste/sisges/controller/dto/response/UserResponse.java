package com.unileste.sisges.controller.dto.response;

import com.unileste.sisges.enums.GenderENUM;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UserResponse {

    private Integer id;
    private String name;
    private String email;
    private String gender;
    private LocalDate birthDate;
    private String register;
}