package com.unileste.sisges.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SearchClassRequest {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String name;
    private Integer page = 0;
    private Integer size = 20;
}