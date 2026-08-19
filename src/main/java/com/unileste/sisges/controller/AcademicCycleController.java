package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.academic.AcademicCycleResponse;
import com.unileste.sisges.controller.dto.academic.AdminPasswordRequest;
import com.unileste.sisges.controller.dto.academic.PendingReleaseResponse;
import com.unileste.sisges.controller.dto.academic.StartAcademicYearRequest;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.AcademicCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academic-cycle")
@RequiredArgsConstructor
public class AcademicCycleController {

    private final AcademicCycleService academicCycleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcademicCycleResponse> getCurrent() {
        return ResponseEntity.ok(academicCycleService.getCurrent());
    }

    @PostMapping("/start-year")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicCycleResponse> startYear(
            @Valid @RequestBody StartAcademicYearRequest request) {
        return ResponseEntity.ok(academicCycleService.startYear(request.getYearEndDate()));
    }

    @PostMapping("/end-trimester")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicCycleResponse> endTrimester(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminPasswordRequest request) {
        return ResponseEntity.ok(academicCycleService.endTrimester(principal.getId(), request.getPassword()));
    }

    @PostMapping("/end-year")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicCycleResponse> endYear(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminPasswordRequest request) {
        return ResponseEntity.ok(academicCycleService.endYear(principal.getId(), request.getPassword()));
    }

    @GetMapping("/pending-releases")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PendingReleaseResponse> pendingReleases(
            @RequestParam(required = false) Integer trimester) {
        return ResponseEntity.ok(academicCycleService.getPendingReleases(trimester));
    }
}

