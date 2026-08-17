package com.unileste.sisges.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    String store(MultipartFile file, String subdir) throws IOException;

    StoredFile load(String key) throws IOException;

    default void delete(String storedPath) {
    }

    record StoredFile(byte[] content, String contentType) {}
}
