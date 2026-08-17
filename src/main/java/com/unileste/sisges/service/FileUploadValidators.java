package com.unileste.sisges.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

final class FileUploadValidators {

    static final List<String> ALLOWED_EXTENSIONS =
            List.of("pdf", "txt", "docx", "doc", "png", "jpg", "jpeg", "gif", "webp");
    static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private FileUploadValidators() {
    }

    static void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande (máx. 10MB)");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo inválido");
        }
        String ext = extension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Use: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1) : "";
    }
}
