package com.unileste.sisges.controller.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGradebookResponse {

    private Integer activityId;
    private Integer classMeetingId;
    private String title;
    private String activityType;
    private Integer trimesterNumber;
    private BigDecimal maxPoints;
    private boolean released;
    private List<StudentGradeLine> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentGradeLine {
        private Integer studentId;
        private Integer userId;
        private String studentName;
        private BigDecimal score;
    }
}

