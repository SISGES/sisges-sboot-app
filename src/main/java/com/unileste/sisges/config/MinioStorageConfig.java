package com.unileste.sisges.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioStorageConfig {
    @Bean
    MinioClient minioClient(MinioStorageProperties properties) {
        require(properties.getEndpoint(), "sisges.minio.endpoint");
        require(properties.getAccessKey(), "sisges.minio.access-key");
        require(properties.getSecretKey(), "sisges.minio.secret-key");
        require(properties.getBucket(), "sisges.minio.bucket");
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    private static void require(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " is required");
        }
    }
}
