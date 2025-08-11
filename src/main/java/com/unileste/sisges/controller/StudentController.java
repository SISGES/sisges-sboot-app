package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.request.CreateStudentDto;
import com.unileste.sisges.controller.dto.request.SearchStudentDto;
import com.unileste.sisges.controller.dto.response.StudentResponseDto;
import com.unileste.sisges.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/all")
    public ResponseEntity<Page<StudentResponseDto>> search(@RequestBody(required = false) SearchStudentDto request) {
        return ResponseEntity.ok(studentService.search(request));
    }

    @PostMapping("/create")
    public ResponseEntity<StudentResponseDto> create(@RequestBody CreateStudentDto request) {
        return ResponseEntity.ok(studentService.create(request));
    }
}