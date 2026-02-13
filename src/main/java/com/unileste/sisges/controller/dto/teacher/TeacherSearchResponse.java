package com.unileste.sisges.controller.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSearchResponse {

    private Integer id;
    private String name;
    private String email;
}