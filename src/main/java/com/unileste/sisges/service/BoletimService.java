package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.boletim.BoletimActivityCellResponse;
import com.unileste.sisges.controller.dto.boletim.BoletimRecoveryRowResponse;
import com.unileste.sisges.controller.dto.boletim.BoletimTrimesterRowResponse;
import com.unileste.sisges.controller.dto.boletim.StudentBoletimResponse;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.ActivityGradeRepository;
import com.unileste.sisges.repository.EvaluativeActivityRepository;
import com.unileste.sisges.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoletimService {

    private final StudentRepository studentRepository;
    private final EvaluativeActivityRepository activityRepository;
    private final ActivityGradeRepository activityGradeRepository;
    private final GradingConfigService gradingConfigService;

    @Transactional(readOnly = true)
    public StudentBoletimResponse getMyBoletim(Integer studentUserId) {
        Student student = studentRepository.findByBaseData_IdAndDeletedAtIsNull(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentUserId));
        if (student.getCurrentClass() == null) {
            return StudentBoletimResponse.builder()
                    .fixedApprovalPercentage(gradingConfigService.fixedApprovalPercentage())
                    .yearMaxPoints(BigDecimal.ZERO)
                    .totalReleasedScore(BigDecimal.ZERO)
                    .eligibleForYearRecovery(false)
                    .trimesters(List.of())
                    .recoveryRow(BoletimRecoveryRowResponse.builder()
                            .trimesterRecoveryScores(Arrays.asList(null, null, null))
                            .yearRecoveryScore(null)
                            .build())
                    .build();
        }

        Integer classId = student.getCurrentClass().getId();
        GradingConfig cfg = gradingConfigService.getCurrentEntity();
        BigDecimal yearMax = new BigDecimal(cfg.getYearMaxPoints());
        BigDecimal thresholdFactor = new BigDecimal("0.70");

        List<EvaluativeActivity> allActivities = activityRepository.findByClassMeeting_SchoolClass_IdAndDeletedAtIsNullOrderByCreatedAtDesc(classId).stream()
                .sorted(Comparator
                        .comparing((EvaluativeActivity a) -> a.getTrimesterNumber() == null ? 99 : a.getTrimesterNumber())
                        .thenComparing(EvaluativeActivity::getCreatedAt))
                .toList();

        Map<Integer, ActivityGrade> gradesByActivity = activityGradeRepository
                .findByStudentIdAndActivity_ClassMeeting_SchoolClass_Id(student.getId(), classId).stream()
                .collect(Collectors.toMap(g -> g.getActivity().getId(), Function.identity(), (a, b) -> b));

        List<BoletimTrimesterRowResponse> trimesters = List.of(
                buildTrimesterRow(1, cfg.getTrimester1MaxPoints(), allActivities, gradesByActivity, thresholdFactor),
                buildTrimesterRow(2, cfg.getTrimester2MaxPoints(), allActivities, gradesByActivity, thresholdFactor),
                buildTrimesterRow(3, cfg.getTrimester3MaxPoints(), allActivities, gradesByActivity, thresholdFactor)
        );

        BigDecimal totalReleased = trimesters.stream()
                .map(BoletimTrimesterRowResponse::getTotalReleasedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean allReleased = trimesters.stream().allMatch(BoletimTrimesterRowResponse::isAllActivitiesReleased);
        boolean eligibleYearRecovery = allReleased && totalReleased.compareTo(yearMax.multiply(thresholdFactor)) < 0;

        List<BigDecimal> trimesterRecovery = Arrays.asList(
                recoveryScore(1, allActivities, gradesByActivity),
                recoveryScore(2, allActivities, gradesByActivity),
                recoveryScore(3, allActivities, gradesByActivity)
        );
        BigDecimal yearRecovery = allActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.RECUPERACAO_ANUAL)
                .filter(EvaluativeActivity::isReleased)
                .max(Comparator.comparing(EvaluativeActivity::getCreatedAt))
                .map(a -> {
                    ActivityGrade g = gradesByActivity.get(a.getId());
                    return g != null ? g.getScore() : null;
                })
                .orElse(null);

        return StudentBoletimResponse.builder()
                .fixedApprovalPercentage(gradingConfigService.fixedApprovalPercentage())
                .yearMaxPoints(yearMax)
                .totalReleasedScore(totalReleased)
                .eligibleForYearRecovery(eligibleYearRecovery)
                .trimesters(trimesters)
                .recoveryRow(BoletimRecoveryRowResponse.builder()
                        .trimesterRecoveryScores(trimesterRecovery)
                        .yearRecoveryScore(yearRecovery)
                        .build())
                .build();
    }

    private BoletimTrimesterRowResponse buildTrimesterRow(
            int trimester,
            int trimesterMaxPoints,
            List<EvaluativeActivity> allActivities,
            Map<Integer, ActivityGrade> gradesByActivity,
            BigDecimal thresholdFactor) {

        List<EvaluativeActivity> trimesterActivities = allActivities.stream()
                .filter(a -> a.getTrimesterNumber() != null && a.getTrimesterNumber() == trimester)
                .filter(a -> a.getActivityType() == ActivityType.PROVA
                        || a.getActivityType() == ActivityType.ATIVIDADE
                        || a.getActivityType() == ActivityType.TRABALHO)
                .toList();

        List<BoletimActivityCellResponse> cells = trimesterActivities.stream()
                .map(a -> {
                    ActivityGrade grade = gradesByActivity.get(a.getId());
                    BigDecimal score = a.isReleased() && grade != null ? grade.getScore() : null;
                    return BoletimActivityCellResponse.builder()
                            .activityId(a.getId())
                            .title(a.getTitle())
                            .activityType(a.getActivityType().name())
                            .maxPoints(a.getMaxPoints())
                            .score(score)
                            .released(a.isReleased())
                            .build();
                })
                .toList();

        BigDecimal total = cells.stream()
                .map(BoletimActivityCellResponse::getScore)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean allReleased = !cells.isEmpty() && cells.stream().allMatch(BoletimActivityCellResponse::isReleased);
        boolean eligibleForRecovery = allReleased
                && total.compareTo(new BigDecimal(trimesterMaxPoints).multiply(thresholdFactor)) < 0;

        return BoletimTrimesterRowResponse.builder()
                .trimester(trimester)
                .trimesterMaxPoints(new BigDecimal(trimesterMaxPoints))
                .activities(cells)
                .totalReleasedScore(total)
                .allActivitiesReleased(allReleased)
                .eligibleForRecovery(eligibleForRecovery)
                .build();
    }

    private BigDecimal recoveryScore(
            int trimester,
            List<EvaluativeActivity> allActivities,
            Map<Integer, ActivityGrade> gradesByActivity) {
        return allActivities.stream()
                .filter(a -> a.getActivityType() == ActivityType.RECUPERACAO_TRIMESTRE)
                .filter(a -> a.getTrimesterNumber() != null && a.getTrimesterNumber() == trimester)
                .filter(EvaluativeActivity::isReleased)
                .max(Comparator.comparing(EvaluativeActivity::getCreatedAt))
                .map(a -> {
                    ActivityGrade g = gradesByActivity.get(a.getId());
                    return g != null ? g.getScore() : null;
                })
                .orElse(null);
    }
}

