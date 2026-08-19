package com.unileste.sisges.service;

import com.unileste.sisges.model.*;
import com.unileste.sisges.repository.ActivityGradeRepository;
import com.unileste.sisges.repository.EvaluativeActivityRepository;
import com.unileste.sisges.repository.GradingConfigRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.StudentYearResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YearProgressionServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private EvaluativeActivityRepository activityRepository;
    @Mock private ActivityGradeRepository activityGradeRepository;
    @Mock private StudentYearResultRepository yearResultRepository;
    @Mock private GradingConfigRepository gradingConfigRepository;

    @InjectMocks
    private YearProgressionService yearProgressionService;

    @Test
    void approvedStudent_isPromotedToNextClass() {
        SchoolClass sourceClass = SchoolClass.builder()
                .id(10)
                .name("A")
                .academicYear("1º ano - Fundamental")
                .build();
        SchoolClass nextClass = SchoolClass.builder()
                .id(11)
                .name("A")
                .academicYear("2º ano - Fundamental")
                .build();
        Student student = Student.builder().id(1).currentClass(sourceClass).build();
        AcademicCycle cycle = AcademicCycle.builder().id(5).status(AcademicCycleStatus.IN_PROGRESS).build();
        GradingConfig cfg = GradingConfig.builder()
                .yearMaxPoints(100)
                .trimester1MaxPoints(33)
                .trimester2MaxPoints(33)
                .trimester3MaxPoints(34)
                .build();

        ClassMeeting meeting = ClassMeeting.builder().schoolClass(sourceClass).build();
        EvaluativeActivity a1 = EvaluativeActivity.builder()
                .id(101)
                .classMeeting(meeting)
                .activityType(ActivityType.PROVA)
                .trimesterNumber(1)
                .released(true)
                .createdAt(LocalDateTime.now())
                .build();
        EvaluativeActivity a2 = EvaluativeActivity.builder()
                .id(102)
                .classMeeting(meeting)
                .activityType(ActivityType.ATIVIDADE)
                .trimesterNumber(2)
                .released(true)
                .createdAt(LocalDateTime.now())
                .build();
        EvaluativeActivity a3 = EvaluativeActivity.builder()
                .id(103)
                .classMeeting(meeting)
                .activityType(ActivityType.TRABALHO)
                .trimesterNumber(3)
                .released(true)
                .createdAt(LocalDateTime.now())
                .build();

        ActivityGrade g1 = ActivityGrade.builder().activity(a1).student(student).score(new BigDecimal("30")).build();
        ActivityGrade g2 = ActivityGrade.builder().activity(a2).student(student).score(new BigDecimal("30")).build();
        ActivityGrade g3 = ActivityGrade.builder().activity(a3).student(student).score(new BigDecimal("30")).build();

        when(yearResultRepository.existsByAcademicCycle_Id(5)).thenReturn(false);
        when(gradingConfigRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(cfg));
        when(studentRepository.findByCurrentClassIsNotNullAndDeletedAtIsNull()).thenReturn(List.of(student));
        when(activityRepository.findByClassMeeting_SchoolClass_IdAndDeletedAtIsNullOrderByCreatedAtDesc(10))
                .thenReturn(List.of(a1, a2, a3));
        when(activityGradeRepository.findByStudentIdAndActivity_ClassMeeting_SchoolClass_Id(1, 10))
                .thenReturn(List.of(g1, g2, g3));
        when(schoolClassRepository.findByNameAndAcademicYearAndDeletedAtIsNull("A", "2º ano - Fundamental"))
                .thenReturn(Optional.of(nextClass));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(yearResultRepository.save(any(StudentYearResult.class))).thenAnswer(inv -> inv.getArgument(0));

        yearProgressionService.processYearEnd(cycle);

        verify(studentRepository).save(student);
        verify(yearResultRepository).save(any(StudentYearResult.class));
    }
}

