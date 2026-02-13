package com.unileste.sisges.controller.dto.discipline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDisciplineRequest {

    @NotBlank(message = "Nome da disciplina é obrigatório")
    @Size(max = 150)
    private String name;

    @Size(max = 5000)
    private String description;
}
