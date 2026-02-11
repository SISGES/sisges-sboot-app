package com.unileste.sisges.controller.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    private String name;
    private String email;
    private String register;
    private String gender;
    private LocalDate initialDate;
    private LocalDate finalDate;
}
