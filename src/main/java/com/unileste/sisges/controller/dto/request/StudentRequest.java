package com.unileste.sisges.controller.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StudentRequest {

    private Integer userId;
    private Integer classId;
    private String register;
    private Integer responsibleId;
}