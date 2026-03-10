package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.activity.CreateEvaluativeActivityRequest;
import com.unileste.sisges.controller.dto.activity.EvaluativeActivityResponse;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.EvaluativeActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class EvaluativeActivityController {

    private final EvaluativeActivityService activityService;

    @GetMapping("/meeting/{classMeetingId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<EvaluativeActivityResponse>> findByClassMeeting(
            @PathVariable Integer classMeetingId) {
        return ResponseEntity.ok(activityService.findByClassMeeting(classMeetingId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EvaluativeActivityResponse>> findForStudent(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(activityService.findForStudent(principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<EvaluativeActivityResponse> create(
            @Valid @RequestBody CreateEvaluativeActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        EvaluativeActivityResponse response = activityService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        activityService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
