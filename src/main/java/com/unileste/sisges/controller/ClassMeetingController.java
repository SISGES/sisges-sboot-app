package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.classmeeting.*;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.ClassMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
public class ClassMeetingController {

    private final ClassMeetingService classMeetingService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassMeetingSearchResponse> create(
            @Valid @RequestBody CreateClassMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClassMeetingSearchResponse response = classMeetingService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<List<ClassMeetingSearchResponse>> search(
            @RequestBody(required = false) ClassMeetingSearchRequest request) {
        List<ClassMeetingSearchResponse> meetings = classMeetingService.search(request != null ? request : ClassMeetingSearchRequest.builder().build());
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ClassMeetingDetailResponse> findById(@PathVariable Integer id) {
        ClassMeetingDetailResponse response = classMeetingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        classMeetingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ClassMeetingDetailResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateClassMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ClassMeetingDetailResponse response = classMeetingService.update(id, request, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/frequency")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<Void> saveFrequency(
            @PathVariable Integer id,
            @Valid @RequestBody FrequencyRequest request) {
        classMeetingService.saveFrequency(id, request);
        return ResponseEntity.noContent().build();
    }
}
