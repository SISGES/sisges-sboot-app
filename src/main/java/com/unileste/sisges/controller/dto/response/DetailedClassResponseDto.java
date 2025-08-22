package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class DetailedClassResponseDto {

    private String name;
    private List<StudentResponseDto> students;
    private LocalDateTime createdAt;
}