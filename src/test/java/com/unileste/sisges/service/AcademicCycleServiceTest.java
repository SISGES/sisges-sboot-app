package com.unileste.sisges.service;

import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.AcademicCycleRepository;
import com.unileste.sisges.repository.EvaluativeActivityRepository;
import com.unileste.sisges.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCycleServiceTest {

    @Mock private AcademicCycleRepository academicCycleRepository;
    @Mock private EvaluativeActivityRepository activityRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AcademicCycleService academicCycleService;

    @Test
    void startYear_setsInProgressAndLocksGrading() {
        AcademicCycle cycle = AcademicCycle.builder()
                .status(AcademicCycleStatus.NOT_STARTED)
                .currentTrimester(1)
                .gradingLocked(false)
                .build();

        when(academicCycleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(cycle));
        when(academicCycleRepository.save(any(AcademicCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = academicCycleService.startYear(LocalDate.now().plusDays(30));

        assertEquals("IN_PROGRESS", response.getStatus());
        assertEquals(1, response.getCurrentTrimester());
    }

    @Test
    void endYear_throwsWhenPasswordInvalid() {
        User admin = User.builder()
                .id(1)
                .userRole("ADMIN")
                .password("encoded")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> academicCycleService.endYear(1, "wrong"));
    }

    @Test
    void endTrimester_throwsWhenThereArePendingTeachers() {
        User admin = User.builder()
                .id(1)
                .userRole("ADMIN")
                .password("encoded")
                .build();
        User teacherUser = User.builder().name("Professor Teste").build();
        Teacher teacher = Teacher.builder().baseData(teacherUser).build();
        ClassMeeting meeting = ClassMeeting.builder().teacher(teacher).build();
        EvaluativeActivity pending = EvaluativeActivity.builder()
                .classMeeting(meeting)
                .released(false)
                .trimesterNumber(1)
                .build();
        AcademicCycle cycle = AcademicCycle.builder()
                .status(AcademicCycleStatus.IN_PROGRESS)
                .currentTrimester(1)
                .gradingLocked(true)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);
        when(academicCycleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(cycle));
        when(activityRepository.findByDeletedAtIsNullAndReleasedFalseAndTrimesterNumber(1)).thenReturn(List.of(pending));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> academicCycleService.endTrimester(1, "admin123"));

        assertEquals(true, ex.getMessage().contains("Professor Teste"));
    }
}

