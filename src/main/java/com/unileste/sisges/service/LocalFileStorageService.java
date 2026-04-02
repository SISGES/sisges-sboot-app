package com.unileste.sisges.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements StorageService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "txt", "docx", "doc", "png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${sisges.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.warn("Could not create upload directory: {}", this.uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subdir) throws IOException {
        validateFile(file);
        Path targetDir = uploadDir.resolve(subdir);
        Files.createDirectories(targetDir);
        String newName = UUID.randomUUID() + "." + getExtension(file.getOriginalFilename());
        Path targetPath = targetDir.resolve(newName);
        Files.copy(file.getInputStream(), targetPath);
        return "/uploads/" + subdir + "/" + newName;
    }

    private void validateFile(MultipartFile file) {
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
        String ext = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Use: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1) : "";
    }
}
