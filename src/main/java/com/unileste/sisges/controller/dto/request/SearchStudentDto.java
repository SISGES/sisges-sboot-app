package com.unileste.sisges.controller.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchStudentDto {
    private LocalDateTime createdAtStart;
    private LocalDateTime createdAtEnd;

    private String register;
    private String name;
    private String responsible1Name;
    private String email;

    private Integer page = 0;
    private Integer size = 20;
}