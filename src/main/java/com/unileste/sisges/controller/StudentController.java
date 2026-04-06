package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.student.DisciplineAbsenceCountResponse;
import com.unileste.sisges.controller.dto.student.MyClassResponse;
import com.unileste.sisges.controller.dto.student.StudentResponse;
import com.unileste.sisges.controller.dto.student.StudentSearchRequest;
import com.unileste.sisges.controller.dto.student.StudentSearchResponse;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentSearchResponse>> searchStudents(
            @RequestBody(required = false) StudentSearchRequest request) {
        List<StudentSearchResponse> students = studentService.searchStudents(request);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/me/turma")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyClassResponse> getMyClass(@AuthenticationPrincipal UserPrincipal principal) {
        MyClassResponse body = studentService.getMyClassForStudentUser(principal.getId());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me/faltas-por-disciplina")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<DisciplineAbsenceCountResponse>> getMyAbsencesByDiscipline(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DisciplineAbsenceCountResponse> body = studentService.getMyAbsenceCountsByDiscipline(principal.getId());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> findById(@PathVariable Integer id) {
        StudentResponse student = studentService.findById(id);
        return ResponseEntity.ok(student);
    }
}