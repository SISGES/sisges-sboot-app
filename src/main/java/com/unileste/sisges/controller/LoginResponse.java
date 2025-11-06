package com.unileste.sisges.controller;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {

    private String token;
    private long expiresIn;
    private String name;
    private String email;
    private String register;
    private String role;
}