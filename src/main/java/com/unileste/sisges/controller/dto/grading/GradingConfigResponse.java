package com.unileste.sisges.controller.dto.grading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingConfigResponse {

    private Integer yearMaxPoints;
    private BigDecimal yearMinPercentage;
    private Integer trimester1MaxPoints;
    private BigDecimal trimester1MinPercentage;
    private Integer trimester2MaxPoints;
    private BigDecimal trimester2MinPercentage;
    private Integer trimester3MaxPoints;
    private BigDecimal trimester3MinPercentage;
    private Integer trimester1PointsProvas;
    private Integer trimester1PointsAtividades;
    private Integer trimester1PointsTrabalhos;
    private Integer trimester2PointsProvas;
    private Integer trimester2PointsAtividades;
    private Integer trimester2PointsTrabalhos;
    private Integer trimester3PointsProvas;
    private Integer trimester3PointsAtividades;
    private Integer trimester3PointsTrabalhos;
}
