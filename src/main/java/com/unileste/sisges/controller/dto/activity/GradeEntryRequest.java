package com.unileste.sisges.controller.dto.activity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeEntryRequest {

    @NotNull(message = "Aluno é obrigatório")
    private Integer studentId;

    @DecimalMin(value = "0.00", message = "Nota deve ser maior ou igual a zero")
    private BigDecimal score;
}

