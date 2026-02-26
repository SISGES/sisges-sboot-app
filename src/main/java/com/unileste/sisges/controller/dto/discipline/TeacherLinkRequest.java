package com.unileste.sisges.controller.dto.discipline;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherLinkRequest {

    @NotNull(message = "ID do professor é obrigatório")
    private Integer teacherId;

    @NotNull(message = "vinculado é obrigatório")
    private Boolean vinculado;
}
