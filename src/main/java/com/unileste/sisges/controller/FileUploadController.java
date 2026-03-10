package com.unileste.sisges.controller;

import com.unileste.sisges.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final StorageService storageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subdir", defaultValue = "general") String subdir) {
        try {
            String path = storageService.store(file, subdir);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao salvar arquivo"));
        }
    }
}
