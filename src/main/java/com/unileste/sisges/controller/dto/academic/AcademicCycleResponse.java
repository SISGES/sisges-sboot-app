package com.unileste.sisges.controller.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCycleResponse {

    private String status;
    private LocalDate yearStartDate;
    private LocalDate yearEndDate;
    private Integer currentTrimester;
    private boolean gradingLocked;
    private LocalDateTime yearStartedAt;
    private LocalDateTime yearFinishedAt;
}

