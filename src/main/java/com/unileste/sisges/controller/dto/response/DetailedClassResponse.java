package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class DetailedClassResponse {

    private String name;
    private List<StudentResponse> students;
    private LocalDateTime createdAt;
}