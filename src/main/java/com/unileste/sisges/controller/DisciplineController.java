package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.discipline.CreateDisciplineRequest;
import com.unileste.sisges.controller.dto.discipline.DisciplineResponse;
import com.unileste.sisges.service.DisciplineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisciplineResponse>> findAll() {
        return ResponseEntity.ok(disciplineService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisciplineResponse> create(@Valid @RequestBody CreateDisciplineRequest request) {
        DisciplineResponse response = disciplineService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisciplineResponse> findById(@PathVariable Integer id) {
        DisciplineResponse response = disciplineService.findById(id);
        return ResponseEntity.ok(response);
    }
}