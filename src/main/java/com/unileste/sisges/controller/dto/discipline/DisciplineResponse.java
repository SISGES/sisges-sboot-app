package com.unileste.sisges.controller.dto.discipline;

import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineResponse {

    private Integer id;
    private String name;
    private String description;
    private List<TeacherSearchResponse> teachers;
}
