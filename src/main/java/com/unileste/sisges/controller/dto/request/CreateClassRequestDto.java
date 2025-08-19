package com.unileste.sisges.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassRequestDto {

    @NotBlank
    private String name;
}