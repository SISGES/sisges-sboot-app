package com.unileste.sisges.controller.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeacherRequest {

    private Integer userId;
    private String register;
}