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
