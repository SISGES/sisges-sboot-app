package com.unileste.sisges.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SearchTeacherRequest {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String register;
    private String name;
    private String email;
    private Integer page = 0;
    private Integer size = 20;
}