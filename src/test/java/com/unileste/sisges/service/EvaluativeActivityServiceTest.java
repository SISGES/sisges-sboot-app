package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.activity.CreateEvaluativeActivityRequest;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EvaluativeActivityServiceTest {

    @Mock private EvaluativeActivityRepository activityRepository;
    @Mock private ActivityGradeRepository activityGradeRepository;
    @Mock private ClassMeetingRepository classMeetingRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private GradingConfigService gradingConfigService;
    @Mock private AcademicCycleService academicCycleService;

    @InjectMocks
    private EvaluativeActivityService service;

    @Test
    void create_failsBeforeAcademicYearStart() {
        doThrow(new BusinessRuleException("O ano letivo ainda não foi iniciado."))
                .when(academicCycleService).assertYearStarted();

        CreateEvaluativeActivityRequest request = CreateEvaluativeActivityRequest.builder()
                .classMeetingId(1)
                .title("Prova 1")
                .activityType("PROVA")
                .trimesterNumber(1)
                .maxPoints(new BigDecimal("10"))
                .build();

        assertThrows(BusinessRuleException.class, () -> service.create(request, 1));
    }
}

