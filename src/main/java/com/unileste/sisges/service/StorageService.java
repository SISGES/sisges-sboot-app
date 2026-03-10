package com.unileste.sisges.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstraction for file storage. Implementations: local disk or Azure Blob Storage.
 * Returns a path/URL that the frontend can use to access the file.
 */
public interface StorageService {

    /**
     * Store a file and return the path or full URL to access it.
     * - Local: returns "/uploads/subdir/filename.ext"
     * - Azure: returns full URL "https://account.blob.core.windows.net/container/subdir/filename.ext"
     */
    String store(MultipartFile file, String subdir) throws IOException;
}
