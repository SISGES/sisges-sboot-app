package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.student.StudentResponse;
import com.unileste.sisges.controller.dto.student.StudentSearchRequest;
import com.unileste.sisges.controller.dto.student.StudentSearchResponse;
import com.unileste.sisges.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> findById(@PathVariable Integer id) {
        StudentResponse student = studentService.findById(id);
        return ResponseEntity.ok(student);
    }
}