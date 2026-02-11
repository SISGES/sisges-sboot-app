package com.unileste.sisges.controller.dto.auth;

import jakarta.validation.constraints.Email;
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
public class ResponsibleData {

    @NotBlank(message = "Nome do responsável é obrigatório")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String alternativePhone;

    @NotBlank(message = "E-mail do responsável é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    @Email(message = "E-mail alternativo inválido")
    private String alternativeEmail;
}
