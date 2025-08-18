package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateStudentDto;
import com.unileste.sisges.controller.dto.request.SearchStudentDto;
import com.unileste.sisges.controller.dto.response.StudentResponseDto;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("StudentControllerTest")
@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    private final String studentName = "STUDENT_NAME";

    @InjectMocks
    private StudentController studentController;
    @Mock
    private StudentService studentService;

    @Test
    @DisplayName("deveRetornarListaPaginada")
    void deveRetornarListaPaginada() {
        SearchStudentDto searchStudentDto = new SearchStudentDto();
        StudentResponseDto studentResponseDto = getResponseDto();
        when(studentService.search(searchStudentDto)).thenReturn(new PageImpl<>(List.of(studentResponseDto)));

        ResponseEntity<Page<StudentResponseDto>> response = studentController.search(searchStudentDto);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSize());
    }

    @Test
    @DisplayName("deveCriarRecurso")
    void deveCriarRecurso() {
        CreateStudentDto createStudentDto = new CreateStudentDto();
        createStudentDto.setName(studentName);
        StudentResponseDto studentResponseDto = getResponseDto();
        when(studentService.create(createStudentDto)).thenReturn(studentResponseDto);

        ResponseEntity<StudentResponseDto> response = studentController.create(createStudentDto);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(studentName, response.getBody().getName());
    }

    @Test
    @DisplayName("deveEncontrarPorId")
    void deveEncontrarPorId() {
        final Integer studentId = 1;
        StudentResponseDto studentResponseDto = StudentResponseDto
                .builder()
                .name(studentName)
                .build();
        when(studentService.findById(studentId)).thenReturn(studentResponseDto);

        ResponseEntity<StudentResponseDto> response = studentController.findById(studentId);

        assertNotNull(response.getBody());
        assertEquals(response.getBody().getName(), studentName);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    @DisplayName("naoDeveEncontrarPorId")
    void naoDeveEncontrarPorId() {
        final Integer nonValidId = 1;
        when(studentService.findById(nonValidId)).thenReturn(null);

        ResponseEntity<StudentResponseDto> response = studentController.findById(nonValidId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.hasBody());
    }

    @Test
    @DisplayName("deveEncontrarStudentEDeletar")
    void deveEncontrarStudentEDeletar() {
        final Integer studentId = 1;
        Student student = Student
                .builder()
                .id(studentId)
                .name(studentName)
                .build();
        when(studentService.delete(studentId)).thenReturn(student);

        ResponseEntity<StudentResponseDto> response = studentController.delete(studentId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.hasBody());
    }

    @Test
    @DisplayName("naoDeveEncontrarStudentAoTentarDeletar")
    void naoDeveEncontrarStudentAoTentarDeletar() {
        final Integer nonValidId = 1;
        when(studentService.delete(nonValidId)).thenReturn(null);

        ResponseEntity<StudentResponseDto> response = studentController.delete(nonValidId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private StudentResponseDto getResponseDto() {
        return StudentResponseDto
                .builder()
                .name(studentName)
                .build();
    }
}