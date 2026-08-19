package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.schoolclass.CreateSchoolClassRequest;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassResponse;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.model.SchoolClass;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.DisciplineRepository;
import com.unileste.sisges.repository.SchoolClassRepository;
import com.unileste.sisges.repository.StudentRepository;
import com.unileste.sisges.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassServiceTest {

    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private DisciplineRepository disciplineRepository;

    @InjectMocks
    private SchoolClassService schoolClassService;

    @Test
    void create_persistsSchoolClass_whenNameIsUnique() {
        CreateSchoolClassRequest request = CreateSchoolClassRequest.builder()
                .name("Turma A")
                .academicYear("6º ano")
                .build();

        when(schoolClassRepository.findByNameAndDeletedAtIsNull("Turma A")).thenReturn(Optional.empty());
        when(schoolClassRepository.save(any())).thenAnswer(inv -> {
            SchoolClass sc = inv.getArgument(0);
            sc.setId(1);
            return sc;
        });

        SchoolClassResponse response = schoolClassService.create(request);

        assertEquals(1, response.getId());
        assertEquals("Turma A", response.getName());
        verify(schoolClassRepository).save(any(SchoolClass.class));
    }

    @Test
    void create_throwsBusinessRuleException_whenNameAlreadyExists() {
        when(schoolClassRepository.findByNameAndDeletedAtIsNull("Turma A"))
                .thenReturn(Optional.of(SchoolClass.builder().id(9).name("Turma A").build()));

        assertThrows(BusinessRuleException.class, () -> schoolClassService.create(CreateSchoolClassRequest.builder()
                .name("Turma A")
                .academicYear("6º ano")
                .build()));
    }

    @Test
    void addStudent_linksStudentToClass() {
        SchoolClass schoolClass = SchoolClass.builder()
                .id(1)
                .name("Turma A")
                .academicYear("6º ano")
                .students(new ArrayList<>())
                .teachers(new ArrayList<>())
                .disciplines(new ArrayList<>())
                .build();
        User user = User.builder().id(5).name("Aluno").email("aluno@test.sisges.local").build();
        Student student = Student.builder().id(3).baseData(user).build();

        when(studentRepository.findByIdAndDeletedAtIsNull(3)).thenReturn(Optional.of(student));
        when(schoolClassRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(schoolClass));
        when(schoolClassRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchoolClassResponse response = schoolClassService.addStudent(1, 3);

        assertEquals(1, response.getStudents().size());
        assertEquals(3, response.getStudents().get(0).getId());
        verify(studentRepository).save(student);
    }
}
