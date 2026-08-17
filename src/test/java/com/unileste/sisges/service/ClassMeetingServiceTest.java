package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.classmeeting.CreateClassMeetingRequest;
import com.unileste.sisges.controller.dto.classmeeting.ClassMeetingSearchResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.model.Discipline;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Teacher;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AttendanceRepository;
import com.unileste.sisges.repository.ClassMeetingRepository;
import com.unileste.sisges.repository.DisciplineRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassMeetingServiceTest {

    @Mock private ClassMeetingRepository classMeetingRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private DisciplineRepository disciplineRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AttendanceRepository attendanceRepository;

    @InjectMocks
    private ClassMeetingService classMeetingService;

    @Test
    void create_persistsMeeting_whenScheduleIsValid() {
        User teacherUser = User.builder().id(2).name("Prof").email("prof@test.sisges.local").build();
        Teacher teacher = Teacher.builder().id(7).baseData(teacherUser).build();
        Discipline discipline = Discipline.builder().id(4).name("Mat").build();
        SchoolClass schoolClass = SchoolClass.builder()
                .id(1)
                .name("Turma")
                .academicYear("6º ano")
                .teachers(new ArrayList<>(List.of(teacher)))
                .disciplines(new ArrayList<>(List.of(discipline)))
                .students(new ArrayList<>())
                .build();

        CreateClassMeetingRequest request = CreateClassMeetingRequest.builder()
                .classId(1)
                .disciplineId(4)
                .date(LocalDate.of(2026, 6, 10))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        when(teacherRepository.findByBaseData_IdAndDeletedAtIsNull(2)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(schoolClass));
        when(disciplineRepository.findByIdAndDeletedAtIsNull(4)).thenReturn(Optional.of(discipline));
        when(classMeetingRepository.findOverlappingMeetings(any(), any(), any(), any())).thenReturn(List.of());
        when(classMeetingRepository.save(any())).thenAnswer(inv -> {
            var meeting = inv.getArgument(0, com.unileste.sisges.model.ClassMeeting.class);
            meeting.setId(99);
            return meeting;
        });

        ClassMeetingSearchResponse response = classMeetingService.create(request, 2);

        assertEquals(99, response.getId());
        assertEquals("Turma", response.getClassName());
    }

    @Test
    void create_throwsBusinessRuleException_whenTeacherNotLinkedToClass() {
        User teacherUser = User.builder().id(2).name("Prof").email("prof@test.sisges.local").build();
        Teacher teacher = Teacher.builder().id(7).baseData(teacherUser).build();
        Discipline discipline = Discipline.builder().id(4).name("Mat").build();
        SchoolClass schoolClass = SchoolClass.builder()
                .id(1)
                .name("Turma")
                .academicYear("6º ano")
                .teachers(new ArrayList<>())
                .disciplines(new ArrayList<>(List.of(discipline)))
                .students(new ArrayList<>())
                .build();

        when(teacherRepository.findByBaseData_IdAndDeletedAtIsNull(2)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(schoolClass));
        when(disciplineRepository.findByIdAndDeletedAtIsNull(4)).thenReturn(Optional.of(discipline));

        assertThrows(BusinessRuleException.class, () -> classMeetingService.create(
                CreateClassMeetingRequest.builder()
                        .classId(1)
                        .disciplineId(4)
                        .date(LocalDate.of(2026, 6, 10))
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(9, 0))
                        .build(),
                2));
    }
}
