package com.unileste.sisges.controller;

import com.unileste.sisges.controller.dto.grading.GradingConfigResponse;
import com.unileste.sisges.controller.dto.grading.UpdateGradingConfigRequest;
import com.unileste.sisges.service.GradingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grading-config")
@RequiredArgsConstructor
public class GradingConfigController {

    private final GradingConfigService gradingConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GradingConfigResponse> get() {
        return ResponseEntity.ok(gradingConfigService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GradingConfigResponse> update(
            @Valid @RequestBody UpdateGradingConfigRequest request) {
        return ResponseEntity.ok(gradingConfigService.update(request));
    }
}
