package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {

    private String token;
    private long expiresIn;
    private UserResponse userResponse;
    private String role;
}