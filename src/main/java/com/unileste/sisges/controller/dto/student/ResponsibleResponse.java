package com.unileste.sisges.controller.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsibleResponse {

    private Integer id;
    private String name;
    private String phone;
    private String email;
}