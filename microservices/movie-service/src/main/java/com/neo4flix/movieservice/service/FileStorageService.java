package com.neo4flix.movieservice.service;

import com.neo4flix.movieservice.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Upload a file to MinIO
     *
     * @param file      The file to upload
     * @param folder    The folder/prefix to store the file under (e.g., "posters", "trailers")
     * @return The object key (path) of the uploaded file (e.g., "posters/uuid.png")
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Cannot upload empty file");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = folder + "/" + UUID.randomUUID() + extension;

            // Upload to MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(filename)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            log.info("Successfully uploaded file: {}", filename);

            // Return just the object key, not a presigned URL
            // Presigned URLs will be generated on-demand when retrieving movies
            return filename;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Get a presigned URL to access a file
     *
     * @param filename The name of the file in MinIO
     * @return A presigned URL valid for 7 days
     */
    public String getFileUrl(String filename) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(filename)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL for file: {}", filename, e);
            throw new RuntimeException("Failed to generate file URL", e);
        }
    }

    /**
     * Delete a file from MinIO
     *
     * @param fileUrlOrKey The URL or object key of the file to delete
     */
    public void deleteFile(String fileUrlOrKey) {
        try {
            String filename;

            // Check if it's a URL or just an object key
            if (fileUrlOrKey.startsWith("http://") || fileUrlOrKey.startsWith("https://")) {
                // Extract filename from URL
                filename = extractFilenameFromUrl(fileUrlOrKey);
            } else {
                // Already an object key
                filename = fileUrlOrKey;
            }

            if (filename != null && !filename.isEmpty()) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(filename)
                                .build()
                );
                log.info("Successfully deleted file: {}", filename);
            }
        } catch (Exception e) {
            log.error("Error deleting file from MinIO", e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Extract filename from MinIO URL
     *
     * @param url The MinIO presigned URL
     * @return The filename/object key
     */
    private String extractFilenameFromUrl(String url) {
        try {
            // URL format: http://localhost:9000/bucket-name/folder/filename?query-params
            String[] parts = url.split("\\?")[0].split("/");
            // Skip protocol, host, port, and bucket name to get the object path
            if (parts.length > 4) {
                StringBuilder filename = new StringBuilder();
                for (int i = 4; i < parts.length; i++) {
                    if (i > 4) filename.append("/");
                    filename.append(parts[i]);
                }
                return filename.toString();
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting filename from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Check if a file exists in MinIO
     *
     * @param filename The filename to check
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String filename) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(filename)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
