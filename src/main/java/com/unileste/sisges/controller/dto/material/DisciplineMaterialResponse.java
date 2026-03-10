package com.unileste.sisges.controller.dto.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineMaterialResponse {

    private Integer id;
    private Integer disciplineId;
    private String disciplineName;
    private Integer classId;
    private String className;
    private String title;
    private String description;
    private String materialType;
    private String filePath;
    private LocalDateTime createdAt;
}
