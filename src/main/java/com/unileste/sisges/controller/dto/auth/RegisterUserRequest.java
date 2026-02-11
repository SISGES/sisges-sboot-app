package com.unileste.sisges.controller.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    private Integer responsibleId;

    private Integer classId;

    @Valid
    private ResponsibleData responsibleData;
}