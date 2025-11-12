package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class StudentResponse {

    private String name;
    private String email;
    private SchoolClassResponse classEntity;
}