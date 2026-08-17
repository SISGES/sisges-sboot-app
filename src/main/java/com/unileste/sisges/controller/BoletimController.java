package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.boletim.StudentBoletimResponse;
import com.unileste.sisges.security.UserPrincipal;
import com.unileste.sisges.service.BoletimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boletim")
@RequiredArgsConstructor
public class BoletimController {

    private final BoletimService boletimService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentBoletimResponse> myBoletim(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boletimService.getMyBoletim(principal.getId()));
    }
}

