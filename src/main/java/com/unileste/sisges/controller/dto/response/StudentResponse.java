package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class StudentResponse {

    private Integer id;
    private Integer userId;
    private Integer currentClass;
    private String register;
    private String name;
    private String email;
}