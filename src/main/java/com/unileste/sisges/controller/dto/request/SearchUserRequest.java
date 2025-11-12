package com.unileste.sisges.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SearchUserRequest {
    private String name;
    private String email;
    private String register;
    private LocalDate initialBirthDate;
    private LocalDate finalBirthDate;
    private String gender;
    private Integer page = 0;
    private Integer size = 20;
}