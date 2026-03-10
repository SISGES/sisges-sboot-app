package com.unileste.sisges.controller.dto.material;

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
public class CreateDisciplineMaterialRequest {

    @NotNull(message = "Disciplina é obrigatória")
    private Integer disciplineId;

    @NotNull(message = "Turma é obrigatória")
    private Integer classId;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255)
    private String title;

    private String description;

    @Size(max = 50)
    private String materialType;

    private String filePath;
}
