package com.unileste.sisges.controller.dto.announcement;

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
public class CreateCommentRequest {

    @NotBlank(message = "Conteúdo do comentário é obrigatório")
    @Size(max = 2000)
    private String content;
}
