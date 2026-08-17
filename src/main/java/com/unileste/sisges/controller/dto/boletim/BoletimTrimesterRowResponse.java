package com.unileste.sisges.controller.dto.boletim;

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
public class BoletimTrimesterRowResponse {

    private Integer trimester;
    private BigDecimal trimesterMaxPoints;
    private List<BoletimActivityCellResponse> activities;
    private BigDecimal totalReleasedScore;
    private boolean allActivitiesReleased;
    private boolean eligibleForRecovery;
}

