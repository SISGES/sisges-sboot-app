package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.material.CreateDisciplineMaterialRequest;
import com.unileste.sisges.controller.dto.material.DisciplineMaterialResponse;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.DisciplineMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class DisciplineMaterialController {

    private final DisciplineMaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<DisciplineMaterialResponse>> list(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer disciplineId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("STUDENT".equals(principal.getRole())) {
            return ResponseEntity.ok(materialService.findForStudent(principal.getId()));
        }
        if ("TEACHER".equals(principal.getRole())) {
            if (classId == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(materialService.findByClassAndDisciplineForTeacher(classId, disciplineId, principal.getId()));
        }
        if (classId != null) {
            return ResponseEntity.ok(materialService.findByClassAndDiscipline(classId, disciplineId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DisciplineMaterialResponse> create(
            @Valid @RequestBody CreateDisciplineMaterialRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DisciplineMaterialResponse response = materialService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        materialService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
