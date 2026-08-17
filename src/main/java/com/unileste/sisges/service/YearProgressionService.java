package com.unileste.sisges.service;

import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.ActivityGradeRepository;
import com.unileste.sisges.repository.EvaluativeActivityRepository;
import com.unileste.sisges.repository.GradingConfigRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.StudentYearResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YearProgressionService {

    private static final List<String> YEAR_SEQUENCE = List.of(
            "1º ano - Fundamental",
            "2º ano - Fundamental",
            "3º ano - Fundamental",
            "4º ano - Fundamental",
            "5º ano - Fundamental",
            "6º ano",
            "7º ano",
            "8º ano",
            "9º ano",
            "1º ano - Médio",
            "2º ano - Médio",
            "3º ano - Médio"
    );

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final EvaluativeActivityRepository activityRepository;
    private final ActivityGradeRepository activityGradeRepository;
    private final StudentYearResultRepository yearResultRepository;
    private final GradingConfigRepository gradingConfigRepository;

    @Transactional
    public void processYearEnd(AcademicCycle cycle) {
        if (cycle.getId() == null) {
            throw new BusinessRuleException("Ciclo acadêmico inválido para processar encerramento.");
        }
        if (yearResultRepository.existsByAcademicCycle_Id(cycle.getId())) {
            return;
        }

        GradingConfig cfg = gradingConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::defaultGradingConfig);
        BigDecimal yearThreshold = new BigDecimal(cfg.getYearMaxPoints()).multiply(new BigDecimal("0.70"));

        List<Student> students = studentRepository.findByCurrentClassIsNotNullAndDeletedAtIsNull();
        for (Student student : students) {
            SchoolClass sourceClass = student.getCurrentClass();
            Integer classId = sourceClass.getId();

            List<EvaluativeActivity> classActivities = activityRepository
                    .findByClassMeeting_SchoolClass_IdAndDeletedAtIsNullOrderByCreatedAtDesc(classId);

            Map<Integer, ActivityGrade> grades = activityGradeRepository
                    .findByStudentIdAndActivity_ClassMeeting_SchoolClass_Id(student.getId(), classId)
                    .stream()
                    .collect(Collectors.toMap(g -> g.getActivity().getId(), Function.identity(), (a, b) -> b));

            BigDecimal t1 = effectiveTrimesterScore(1, cfg.getTrimester1MaxPoints(), classActivities, grades);
            BigDecimal t2 = effectiveTrimesterScore(2, cfg.getTrimester2MaxPoints(), classActivities, grades);
            BigDecimal t3 = effectiveTrimesterScore(3, cfg.getTrimester3MaxPoints(), classActivities, grades);
            BigDecimal baseYearScore = t1.add(t2).add(t3);

            BigDecimal yearRecovery = findYearRecoveryScore(classActivities, grades);
            boolean approved = baseYearScore.compareTo(yearThreshold) >= 0;
            BigDecimal finalScore = baseYearScore;
            if (!approved && yearRecovery != null) {
                BigDecimal missing = yearThreshold.subtract(baseYearScore);
                if (missing.compareTo(BigDecimal.ZERO) < 0) {
                    missing = BigDecimal.ZERO;
                }
                BigDecimal requiredInRecovery = new BigDecimal("70").add(missing);
                if (yearRecovery.compareTo(requiredInRecovery) >= 0) {
                    approved = true;
                    finalScore = yearThreshold;
                }
            }

            SchoolClass nextClass = null;
            boolean promoted = false;
            if (approved) {
                nextClass = resolveNextClass(sourceClass);
                promoted = true;
                student.setCurrentClass(nextClass); // null means concluded last year
                studentRepository.save(student);
            }

            StudentYearResult result = StudentYearResult.builder()
                    .academicCycle(cycle)
                    .student(student)
                    .sourceClass(sourceClass)
                    .nextClass(nextClass)
                    .baseScore(baseYearScore)
                    .finalScore(finalScore)
                    .yearRecoveryScore(yearRecovery)
                    .approved(approved)
                    .promoted(promoted)
                    .build();
            yearResultRepository.save(result);
        }
    }

    private BigDecimal effectiveTrimesterScore(
            int trimester,
            int trimesterMaxPoints,
            List<EvaluativeActivity> activities,
            Map<Integer, ActivityGrade> grades) {
        List<EvaluativeActivity> normal = activities.stream()
                .filter(a -> a.getTrimesterNumber() != null && a.getTrimesterNumber() == trimester)
                .filter(a -> a.getActivityType() == ActivityType.PROVA
                        || a.getActivityType() == ActivityType.ATIVIDADE
                        || a.getActivityType() == ActivityType.TRABALHO)
                .toList();

        BigDecimal base = normal.stream()
                .filter(EvaluativeActivity::isReleased)
                .map(a -> {
                    ActivityGrade g = grades.get(a.getId());
                    return g != null && g.getScore() != null ? g.getScore() : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal threshold = new BigDecimal(trimesterMaxPoints).multiply(new BigDecimal("0.70"));
        if (base.compareTo(threshold) >= 0) {
            return base;
        }
        if (normal.isEmpty() || normal.stream().anyMatch(a -> !a.isReleased())) {
            return base;
        }

        BigDecimal recoveryScore = findTrimesterRecoveryScore(trimester, activities, grades);
        if (recoveryScore == null) {
            return base;
        }

        BigDecimal missing = threshold.subtract(base);
        if (missing.compareTo(BigDecimal.ZERO) < 0) {
            missing = BigDecimal.ZERO;
        }
        BigDecimal requiredInRecovery = new BigDecimal("70").add(missing);
        if (recoveryScore.compareTo(requiredInRecovery) >= 0) {
            return threshold;
        }
        return base;
    }

    private BigDecimal findTrimesterRecoveryScore(
            int trimester,
            List<EvaluativeActivity> activities,
            Map<Integer, ActivityGrade> grades) {
        return activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.RECUPERACAO_TRIMESTRE)
                .filter(a -> a.getTrimesterNumber() != null && a.getTrimesterNumber() == trimester)
                .filter(EvaluativeActivity::isReleased)
                .max(Comparator.comparing(EvaluativeActivity::getCreatedAt))
                .map(a -> {
                    ActivityGrade g = grades.get(a.getId());
                    return g != null ? g.getScore() : null;
                })
                .orElse(null);
    }

    private BigDecimal findYearRecoveryScore(
            List<EvaluativeActivity> activities,
            Map<Integer, ActivityGrade> grades) {
        return activities.stream()
                .filter(a -> a.getActivityType() == ActivityType.RECUPERACAO_ANUAL)
                .filter(EvaluativeActivity::isReleased)
                .max(Comparator.comparing(EvaluativeActivity::getCreatedAt))
                .map(a -> {
                    ActivityGrade g = grades.get(a.getId());
                    return g != null ? g.getScore() : null;
                })
                .orElse(null);
    }

    private SchoolClass resolveNextClass(SchoolClass sourceClass) {
        int currentIndex = YEAR_SEQUENCE.indexOf(sourceClass.getAcademicYear());
        if (currentIndex < 0) {
            throw new BusinessRuleException("Série atual inválida para progressão: " + sourceClass.getAcademicYear());
        }
        if (currentIndex == YEAR_SEQUENCE.size() - 1) {
            return null; // concluinte
        }
        String nextAcademicYear = YEAR_SEQUENCE.get(currentIndex + 1);
        return schoolClassRepository.findByNameAndAcademicYearAndDeletedAtIsNull(sourceClass.getName(), nextAcademicYear)
                .or(() -> schoolClassRepository.findByAcademicYearAndDeletedAtIsNullOrderByNameAsc(nextAcademicYear).stream().findFirst())
                .orElseThrow(() -> new BusinessRuleException(
                        "Não existe turma de destino para " + sourceClass.getName()
                                + " (" + sourceClass.getAcademicYear() + " -> " + nextAcademicYear + ")."));
    }

    private GradingConfig defaultGradingConfig() {
        int t1 = 33;
        int t2 = 33;
        int t3 = 34;
        return GradingConfig.builder()
                .yearMaxPoints(100)
                .trimester1MaxPoints(t1)
                .trimester2MaxPoints(t2)
                .trimester3MaxPoints(t3)
                .build();
    }
}

