package com.unileste.sisges.controller.dto.event;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class SchoolEventResponse {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime eventAt;
    private String audience;
    private Integer classId;
    private String className;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
