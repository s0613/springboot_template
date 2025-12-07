package com.template.app.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Local file storage implementation for development/testing.
 * Files are stored in a local directory.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${file.storage.local.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file, String path, String filename) {
        try {
            String filePath = buildPath(path, filename);
            Path targetPath = Paths.get(basePath, filePath);

            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());

            // Write file
            file.transferTo(targetPath);

            log.info("File uploaded to local storage: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to upload file to local storage", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public String upload(byte[] bytes, String path, String filename, String contentType) {
        try {
            String filePath = buildPath(path, filename);
            Path targetPath = Paths.get(basePath, filePath);

            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());

            // Write file
            Files.write(targetPath, bytes);

            log.info("File uploaded to local storage: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to upload file to local storage", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public InputStream download(String filePath) {
        try {
            Path path = Paths.get(basePath, filePath);
            return new FileInputStream(path.toFile());
        } catch (FileNotFoundException e) {
            log.error("File not found in local storage: {}", filePath, e);
            throw new RuntimeException("File not found", e);
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            Path path = Paths.get(basePath, filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("File deleted from local storage: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete file from local storage: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String filePath) {
        Path path = Paths.get(basePath, filePath);
        return Files.exists(path);
    }

    @Override
    public String getPublicUrl(String filePath) {
        return baseUrl + "/" + filePath;
    }

    @Override
    public String getPresignedUrl(String filePath, Duration duration) {
        // Local storage doesn't support presigned URLs, just return public URL
        log.warn("Presigned URLs are not supported in local storage mode");
        return getPublicUrl(filePath);
    }

    @Override
    public String getPresignedUploadUrl(String filePath, String contentType, Duration duration) {
        // Local storage doesn't support presigned upload URLs
        log.warn("Presigned upload URLs are not supported in local storage mode");
        throw new UnsupportedOperationException("Presigned upload URLs are not supported in local storage mode");
    }

    private String buildPath(String path, String filename) {
        if (path == null || path.isEmpty()) {
            return filename;
        }
        String cleanPath = path.replaceAll("^/+|/+$", "");
        return cleanPath + "/" + filename;
    }
}
