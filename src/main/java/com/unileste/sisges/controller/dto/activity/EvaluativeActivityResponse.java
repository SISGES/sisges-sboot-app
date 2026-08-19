package com.unileste.sisges.controller.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluativeActivityResponse {

    private Integer id;
    private Integer classMeetingId;
    private String title;
    private String description;
    private String filePath;
    private String activityType;
    private Integer trimesterNumber;
    private BigDecimal maxPoints;
    private boolean released;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
}
