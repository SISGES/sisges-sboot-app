package com.unileste.sisges.controller.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 255)
    @Pattern(regexp = "^.+@sisges\\.com$", message = "O e-mail deve pertencer ao domínio @sisges.com")
    private String email;

    @NotBlank(message = "Registro/matrícula é obrigatório")
    @Size(max = 50)
    private String register;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;

    @NotNull(message = "Data de nascimento é obrigatória")
    private LocalDate birthDate;

    @NotBlank(message = "Gênero é obrigatório")
    @Size(max = 20)
    private String gender;

    @NotBlank(message = "Papel do usuário é obrigatório")
    @Pattern(regexp = "^(ADMIN|TEACHER|STUDENT)$", message = "Papel deve ser ADMIN, TEACHER ou STUDENT")
    private String role;

    /**
     * Para STUDENT: ID do responsável legal (opcional se for fornecido responsibleData).
     */
    private Integer responsibleId;

    /**
     * Para STUDENT: ID da turma (opcional).
     */
    private Integer classId;

    /**
     * Para STUDENT: dados do responsável para criação em conjunto (opcional).
     */
    @Valid
    private ResponsibleData responsibleData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsibleData {
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
        @Pattern(regexp = "^.+@sisges\\.com$", message = "O e-mail deve pertencer ao domínio @sisges.com")
        private String email;

        @Size(max = 255)
        @Email(message = "E-mail alternativo inválido")
        @Pattern(regexp = "^$|^.+@sisges\\.com$", message = "O e-mail alternativo deve pertencer ao domínio @sisges.com")
        private String alternativeEmail;
    }
}
