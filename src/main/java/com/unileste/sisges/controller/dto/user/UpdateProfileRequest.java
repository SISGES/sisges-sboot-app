package com.unileste.sisges.controller.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 255)
    private String name;
    @Size(min = 8, max = 100)
    private String password;
    @Size(max = 500)
    private String profileImagePath;
}
