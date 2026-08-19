package com.unileste.sisges.controller.dto.grading;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGradingConfigRequest {

    public UpdateGradingConfigRequest(
            Integer trimester1MaxPoints,
            Integer trimester2MaxPoints,
            Integer trimester3MaxPoints,
            Integer trimester1PointsProvas,
            Integer trimester1PointsAtividades,
            Integer trimester1PointsTrabalhos,
            Integer trimester2PointsProvas,
            Integer trimester2PointsAtividades,
            Integer trimester2PointsTrabalhos,
            Integer trimester3PointsProvas,
            Integer trimester3PointsAtividades,
            Integer trimester3PointsTrabalhos) {
        this.yearMaxPoints = trimester1MaxPoints + trimester2MaxPoints + trimester3MaxPoints;
        this.yearMinPercentage = new BigDecimal("70.00");
        this.trimester1MaxPoints = trimester1MaxPoints;
        this.trimester1MinPercentage = new BigDecimal("70.00");
        this.trimester2MaxPoints = trimester2MaxPoints;
        this.trimester2MinPercentage = new BigDecimal("70.00");
        this.trimester3MaxPoints = trimester3MaxPoints;
        this.trimester3MinPercentage = new BigDecimal("70.00");
        this.trimester1PointsProvas = trimester1PointsProvas;
        this.trimester1PointsAtividades = trimester1PointsAtividades;
        this.trimester1PointsTrabalhos = trimester1PointsTrabalhos;
        this.trimester2PointsProvas = trimester2PointsProvas;
        this.trimester2PointsAtividades = trimester2PointsAtividades;
        this.trimester2PointsTrabalhos = trimester2PointsTrabalhos;
        this.trimester3PointsProvas = trimester3PointsProvas;
        this.trimester3PointsAtividades = trimester3PointsAtividades;
        this.trimester3PointsTrabalhos = trimester3PointsTrabalhos;
    }

    @NotNull @Min(10) @Max(1000)
    private Integer yearMaxPoints;

    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal yearMinPercentage;

    @NotNull @Min(1)
    private Integer trimester1MaxPoints;

    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal trimester1MinPercentage;

    @NotNull @Min(1)
    private Integer trimester2MaxPoints;

    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal trimester2MinPercentage;

    @NotNull @Min(1)
    private Integer trimester3MaxPoints;

    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal trimester3MinPercentage;

    @NotNull @Min(1)
    private Integer trimester1PointsProvas;

    @NotNull @Min(1)
    private Integer trimester1PointsAtividades;

    @NotNull @Min(1)
    private Integer trimester1PointsTrabalhos;

    @NotNull @Min(1)
    private Integer trimester2PointsProvas;

    @NotNull @Min(1)
    private Integer trimester2PointsAtividades;

    @NotNull @Min(1)
    private Integer trimester2PointsTrabalhos;

    @NotNull @Min(1)
    private Integer trimester3PointsProvas;

    @NotNull @Min(1)
    private Integer trimester3PointsAtividades;

    @NotNull @Min(1)
    private Integer trimester3PointsTrabalhos;
}
