package com.unileste.sisges.service;

import com.unileste.sisges.config.MinioStorageProperties;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {
    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @Override
    public String store(MultipartFile file, String subdir) throws IOException {
        FileUploadValidators.validate(file);
        String extension = FileUploadValidators.extension(file.getOriginalFilename());
        String key = subdir + "/" + UUID.randomUUID() + "." + extension;
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket()).object(key)
                    .stream(file.getInputStream(), file.getSize(), 10 * 1024 * 1024L)
                    .contentType(contentType).build());
            log.debug("Uploaded {} to MinIO bucket {}/{}", file.getOriginalFilename(), properties.getBucket(), key);
            return "/api/files/" + key;
        } catch (Exception exception) {
            throw new IOException("Could not store file in MinIO", exception);
        }
    }

    @Override
    public StoredFile load(String key) throws IOException {
        String normalizedKey = StoredPathUtils.normalizeStorageKey(key);
        if (normalizedKey == null) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        try (GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket()).object(normalizedKey).build())) {
            String contentType = response.headers().get("Content-Type");
            return new StoredFile(response.readAllBytes(), contentType == null ? "application/octet-stream" : contentType);
        } catch (Exception exception) {
            throw new IOException("Could not load file from MinIO", exception);
        }
    }

    @Override
    public void delete(String storedPath) {
        String key = StoredPathUtils.extractStorageKey(storedPath);
        if (key == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket()).object(key).build());
            log.debug("Deleted MinIO object {}/{}", properties.getBucket(), key);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete file from MinIO: " + storedPath, exception);
        }
    }
}
