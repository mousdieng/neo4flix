package com.neo4flix.userservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    @ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = true)
    public MinioClient minioClient() {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getUrl())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();

            // Create bucket if it doesn't exist
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .build()
                );
                log.info("Created MinIO bucket: {}", minioProperties.getBucketName());
            } else {
                log.info("MinIO bucket already exists: {}", minioProperties.getBucketName());
            }

            return minioClient;
        } catch (Exception e) {
            log.warn("MinIO is not available. File storage functionality will be disabled. Error: {}", e.getMessage());
            return null;
        }
    }
}
