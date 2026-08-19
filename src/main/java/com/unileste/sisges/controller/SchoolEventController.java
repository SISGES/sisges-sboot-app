package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.event.*;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.SchoolEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/events") @RequiredArgsConstructor
public class SchoolEventController {
    private final SchoolEventService service;
    @GetMapping public List<SchoolEventResponse> upcoming(@AuthenticationPrincipal UserPrincipal principal) {
        return service.upcoming(principal.getId(), principal.getRole());
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolEventResponse> create(@Valid @RequestBody CreateSchoolEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal.getId()));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
