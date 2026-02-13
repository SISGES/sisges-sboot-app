package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.schoolclass.CreateSchoolClassRequest;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassResponse;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassSearchRequest;
import com.unileste.sisges.controller.dto.schoolclass.SchoolClassSearchResponse;
import com.unileste.sisges.service.SchoolClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        SchoolClassResponse response = schoolClassService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SchoolClassSearchResponse>> search(
            @RequestBody(required = false) SchoolClassSearchRequest request) {
        List<SchoolClassSearchResponse> classes = schoolClassService.search(request);
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> findById(@PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/teacher/add/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> addTeacher(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.addTeacher(classId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/student/add/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> addStudent(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.addStudent(classId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/teacher/remove/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> removeTeacher(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.removeTeacher(classId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/student/remove/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> removeStudent(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.removeStudent(classId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/discipline/add/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> addDiscipline(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.addDiscipline(classId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/discipline/remove/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolClassResponse> removeDiscipline(
            @PathVariable Integer classId,
            @PathVariable Integer id) {
        SchoolClassResponse response = schoolClassService.removeDiscipline(classId, id);
        return ResponseEntity.ok(response);
    }
}