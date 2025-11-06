package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ClassResponse {

    private Integer id;
    private String name;
    private LocalDateTime createdAt;
}