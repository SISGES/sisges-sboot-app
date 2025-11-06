package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.StudentRequest;
import com.unileste.sisges.controller.dto.request.SearchStudentDto;
import com.unileste.sisges.controller.dto.response.StudentResponse;
import com.unileste.sisges.model.Student;
import com.unileste.sisges.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/search")
    public ResponseEntity<Page<StudentResponse>> search(@RequestBody(required = false) SearchStudentDto request) {
        return ResponseEntity.ok(studentService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> findById(@PathVariable Integer id) {
        StudentResponse response = studentService.findById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<StudentResponse> delete(@PathVariable Integer id) {
        Student response = studentService.delete(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().build();
    }
}