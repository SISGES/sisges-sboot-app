package com.unileste.sisges.controller.dto.academic;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPasswordRequest {

    @NotBlank(message = "Senha é obrigatória")
    private String password;
}

