package com.unileste.sisges.controller.dto.discipline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDisciplineRequest {

    @NotBlank(message = "Nome da disciplina é obrigatório")
    @Size(max = 150)
    private String name;

    @Size(max = 5000)
    private String description;

    @Valid
    private List<TeacherLinkRequest> teachers;
}
