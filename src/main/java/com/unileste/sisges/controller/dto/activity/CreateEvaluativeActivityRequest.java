package com.unileste.sisges.controller.dto.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEvaluativeActivityRequest {

    @NotNull(message = "Aula é obrigatória")
    private Integer classMeetingId;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255)
    private String title;

    private String description;

    private String filePath;

    @Pattern(
            regexp = "^(PROVA|ATIVIDADE|TRABALHO|RECUPERACAO_TRIMESTRE|RECUPERACAO_ANUAL)$",
            message = "Tipo de atividade inválido")
    @Builder.Default
    private String activityType = "ATIVIDADE";

    @Min(1)
    @Max(3)
    private Integer trimesterNumber;

    @NotNull(message = "Pontuação máxima é obrigatória")
    @DecimalMin(value = "0.01", message = "Pontuação máxima deve ser maior que zero")
    private BigDecimal maxPoints;
}
