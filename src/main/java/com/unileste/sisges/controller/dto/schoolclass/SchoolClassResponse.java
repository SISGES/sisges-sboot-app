package com.unileste.sisges.controller.dto.schoolclass;

import com.unileste.sisges.controller.dto.discipline.DisciplineSimpleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassResponse {

    private Integer id;
    private String name;
    private String academicYear;
    private List<StudentSimpleResponse> students;
    private List<TeacherSimpleResponse> teachers;
    private List<DisciplineSimpleResponse> disciplines;
}