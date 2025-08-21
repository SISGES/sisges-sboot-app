package com.unileste.sisges.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDto {

    private String email;
    private String password;
}