package com.unileste.sisges.controller.dto.discipline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineSimpleResponse {

    private Integer id;
    private String name;
}
