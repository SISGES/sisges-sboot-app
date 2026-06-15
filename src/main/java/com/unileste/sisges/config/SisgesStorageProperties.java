package com.unileste.sisges.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sisges.storage")
public class SisgesStorageProperties {

    private String type = "local";

    private final S3 s3 = new S3();

    @Data
    public static class S3 {
        private String bucket;
        private String region = "us-east-2";
        private String publicBaseUrl;
    }
}
