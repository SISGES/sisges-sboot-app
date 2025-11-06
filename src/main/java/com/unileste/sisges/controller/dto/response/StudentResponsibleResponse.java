package com.unileste.sisges.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentResponsibleResponse {

    private Integer id;
    private String name;
    private String phone;
    private String alternativePhone;
    private String email;
    private String alternativeEmail;
}