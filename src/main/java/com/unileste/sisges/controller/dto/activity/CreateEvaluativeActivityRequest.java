package com.unileste.sisges.controller.dto.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
