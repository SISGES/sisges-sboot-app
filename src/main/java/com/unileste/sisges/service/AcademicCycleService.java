package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.academic.AcademicCycleResponse;
import com.unileste.sisges.controller.dto.academic.PendingReleaseResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.exception.ResourceNotFoundException;
import com.unileste.sisges.model.AcademicCycle;
import com.unileste.sisges.model.AcademicCycleStatus;
import com.unileste.sisges.model.EvaluativeActivity;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AcademicCycleRepository;
import com.unileste.sisges.repository.EvaluativeActivityRepository;
import com.unileste.sisges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicCycleService {

    public static final BigDecimal FIXED_APPROVAL_PERCENTAGE = new BigDecimal("70.00");

    private final AcademicCycleRepository academicCycleRepository;
    private final EvaluativeActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final YearProgressionService yearProgressionService;

    @Transactional
    public AcademicCycleResponse getCurrent() {
        return toResponse(getOrCreateInternal());
    }

    @Transactional
    public AcademicCycleResponse startYear(LocalDate yearEndDate) {
        if (yearEndDate == null) {
            throw new BusinessRuleException("Data final do ano letivo é obrigatória.");
        }
        if (!yearEndDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data final deve ser maior que hoje.");
        }

        AcademicCycle cycle = getOrCreateInternal();
        if (cycle.getStatus() == AcademicCycleStatus.IN_PROGRESS) {
            throw new BusinessRuleException("O ano letivo já foi iniciado.");
        }

        cycle.setStatus(AcademicCycleStatus.IN_PROGRESS);
        cycle.setYearStartDate(LocalDate.now());
        cycle.setYearEndDate(yearEndDate);
        cycle.setCurrentTrimester(1);
        cycle.setGradingLocked(true);
        cycle.setYearStartedAt(LocalDateTime.now());
        cycle.setYearFinishedAt(null);
        return toResponse(academicCycleRepository.save(cycle));
    }

    @Transactional
    public AcademicCycleResponse endTrimester(Integer userId, String password) {
        validateAdminPassword(userId, password);
        AcademicCycle cycle = requireInProgress();

        List<String> pending = pendingTeacherNames(cycle.getCurrentTrimester());
        if (!pending.isEmpty()) {
            throw new BusinessRuleException("Ainda existem notas não liberadas: " + String.join(", ", pending));
        }

        if (cycle.getCurrentTrimester() < 3) {
            cycle.setCurrentTrimester(cycle.getCurrentTrimester() + 1);
        }
        return toResponse(academicCycleRepository.save(cycle));
    }

    @Transactional
    public AcademicCycleResponse endYear(Integer userId, String password) {
        validateAdminPassword(userId, password);
        AcademicCycle cycle = requireInProgress();

        List<String> pending = pendingTeacherNames(null);
        if (!pending.isEmpty()) {
            throw new BusinessRuleException("Ainda existem notas não liberadas: " + String.join(", ", pending));
        }

        yearProgressionService.processYearEnd(cycle);
        cycle.setStatus(AcademicCycleStatus.FINISHED);
        cycle.setYearFinishedAt(LocalDateTime.now());
        return toResponse(academicCycleRepository.save(cycle));
    }

    @Transactional(readOnly = true)
    public PendingReleaseResponse getPendingReleases(Integer trimester) {
        return PendingReleaseResponse.builder()
                .trimester(trimester)
                .teachers(pendingTeacherNames(trimester))
                .build();
    }

    @Transactional(readOnly = true)
    public void assertYearStarted() {
        AcademicCycle cycle = findExistingInternal()
                .orElseThrow(() -> new BusinessRuleException("O ano letivo ainda não foi iniciado."));
        if (cycle.getStatus() != AcademicCycleStatus.IN_PROGRESS) {
            throw new BusinessRuleException("O ano letivo ainda não foi iniciado.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isGradingLocked() {
        return findExistingInternal()
                .map(AcademicCycle::isGradingLocked)
                .orElse(false);
    }

    private AcademicCycle requireInProgress() {
        AcademicCycle cycle = getOrCreateInternal();
        if (cycle.getStatus() != AcademicCycleStatus.IN_PROGRESS) {
            throw new BusinessRuleException("O ano letivo não está em andamento.");
        }
        return cycle;
    }

    private java.util.Optional<AcademicCycle> findExistingInternal() {
        return academicCycleRepository.findFirstByOrderByIdAsc();
    }

    private AcademicCycle getOrCreateInternal() {
        return findExistingInternal()
                .orElseGet(() -> academicCycleRepository.save(AcademicCycle.builder()
                        .status(AcademicCycleStatus.NOT_STARTED)
                        .currentTrimester(1)
                        .gradingLocked(false)
                        .build()));
    }

    private AcademicCycleResponse toResponse(AcademicCycle cycle) {
        return AcademicCycleResponse.builder()
                .status(cycle.getStatus().name())
                .yearStartDate(cycle.getYearStartDate())
                .yearEndDate(cycle.getYearEndDate())
                .currentTrimester(cycle.getCurrentTrimester())
                .gradingLocked(cycle.isGradingLocked())
                .yearStartedAt(cycle.getYearStartedAt())
                .yearFinishedAt(cycle.getYearFinishedAt())
                .build();
    }

    private List<String> pendingTeacherNames(Integer trimester) {
        List<EvaluativeActivity> pending = trimester == null
                ? activityRepository.findByDeletedAtIsNullAndReleasedFalse()
                : activityRepository.findByDeletedAtIsNullAndReleasedFalseAndTrimesterNumber(trimester);

        return pending.stream()
                .map(a -> a.getClassMeeting().getTeacher())
                .filter(t -> t != null && t.getBaseData() != null)
                .map(t -> t.getBaseData().getName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void validateAdminPassword(Integer userId, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Senha do administrador é obrigatória.");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        if (!"ADMIN".equalsIgnoreCase(user.getUserRole())) {
            throw new BusinessRuleException("Apenas administradores podem executar esta ação.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessRuleException("Senha inválida.");
        }
    }
}

