package com.unileste.sisges.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class SchoolClassSimpleResponse {

    private String name;
    private List<Map<String, String>> teachersNameAndEmail;
    private List<Map<String, String>> studentsNameAndEmail;
}