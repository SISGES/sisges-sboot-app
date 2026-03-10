package com.unileste.sisges.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnExpression("!'${azure.storage.connection-string:}'.isEmpty()")
public class AzureBlobStorageService implements StorageService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "txt", "docx", "doc", "png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final BlobContainerClient containerClient;

    public AzureBlobStorageService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container:sisgesfiles}") String containerName) {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!this.containerClient.exists()) {
            this.containerClient.create();
            log.info("Created Azure Blob container: {}", containerName);
        }
    }

    @Override
    public String store(MultipartFile file, String subdir) throws IOException {
        validateFile(file);
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        String blobName = subdir + "/" + UUID.randomUUID() + "." + ext;
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        return blobClient.getBlobUrl();
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
