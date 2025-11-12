package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class SchoolClassResponse {

    private Integer id;
    private String name;
    private int teacherCount;
    private int studentCount;
    private LocalDateTime createdAt;
}