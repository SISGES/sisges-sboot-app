package com.unileste.sisges.controller.dto.schoolclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSimpleResponse {

    private Integer id;
    private String name;
    private String email;
}