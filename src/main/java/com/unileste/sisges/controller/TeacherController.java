package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.teacher.TeacherResponse;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchRequest;
import com.unileste.sisges.controller.dto.teacher.TeacherSearchResponse;
import com.unileste.sisges.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherSearchResponse>> searchTeachers(
            @RequestBody(required = false) TeacherSearchRequest request) {
        List<TeacherSearchResponse> teachers = teacherService.searchTeachers(request);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> findById(@PathVariable Integer id) {
        TeacherResponse teacher = teacherService.findById(id);
        return ResponseEntity.ok(teacher);
    }
}