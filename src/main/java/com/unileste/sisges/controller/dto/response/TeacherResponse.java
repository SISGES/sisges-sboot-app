package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class TeacherResponse {

    private Integer id;
    private Integer userId;
    private String name;
    private String email;
    private List<SchoolClassResponse> classes;
}