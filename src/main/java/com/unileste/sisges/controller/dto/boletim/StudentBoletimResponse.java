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
public class StudentBoletimResponse {

    private BigDecimal fixedApprovalPercentage;
    private BigDecimal yearMaxPoints;
    private BigDecimal totalReleasedScore;
    private boolean eligibleForYearRecovery;
    private List<BoletimTrimesterRowResponse> trimesters;
    private BoletimRecoveryRowResponse recoveryRow;
}

