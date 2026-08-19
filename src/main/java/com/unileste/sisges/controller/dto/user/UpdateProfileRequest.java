package com.unileste.sisges.controller.dto.user;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 255)
    private String name;
    @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,100}$", message = "Senha deve conter uma letra maiúscula e um símbolo")
    private String password;
    @Size(max = 500)
    private String profileImagePath;
}
