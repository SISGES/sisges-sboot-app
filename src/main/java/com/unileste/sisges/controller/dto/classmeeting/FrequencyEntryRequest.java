package com.unileste.sisges.controller.dto.classmeeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrequencyEntryRequest {

    @NotNull(message = "ID do aluno é obrigatório")
    private Integer studentId;

    @NotBlank(message = "Status de presença é obrigatório")
    @Pattern(regexp = "^(P|F)$", message = "Status deve ser P (presente) ou F (faltoso)")
    private String status;
}
