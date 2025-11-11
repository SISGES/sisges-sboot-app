package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.response.TeacherResponse;
import com.unileste.sisges.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> findById(@PathVariable Integer id) {
        TeacherResponse response = teacherService.findById(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}