package com.unileste.sisges.controller.dto.schoolclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassSearchRequest {

    private String name;
    private String academicYear;
}