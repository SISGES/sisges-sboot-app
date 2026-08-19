package com.unileste.sisges.controller.dto.boletim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoletimActivityCellResponse {

    private Integer activityId;
    private String title;
    private String activityType;
    private BigDecimal maxPoints;
    private BigDecimal score;
    private boolean released;
}

