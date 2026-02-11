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
}