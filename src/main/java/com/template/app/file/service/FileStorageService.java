package com.template.app.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstract interface for file storage operations.
 * Implementations can use S3, local storage, or other backends.
 */
public interface FileStorageService {

    /**
     * Upload a file to storage
     *
     * @param file     the file to upload
     * @param path     the storage path (e.g., "profiles/123")
     * @param filename the filename to use
     * @return the full path/key where the file was stored
     */
    String upload(MultipartFile file, String path, String filename);

    /**
     * Upload a file with bytes
     *
     * @param bytes       file content as bytes
     * @param path        the storage path
     * @param filename    the filename to use
     * @param contentType MIME type of the file
     * @return the full path/key where the file was stored
     */
    String upload(byte[] bytes, String path, String filename, String contentType);

    /**
     * Download a file from storage
     *
     * @param filePath the path/key of the file
     * @return InputStream of the file content
     */
    InputStream download(String filePath);

    /**
     * Delete a file from storage
     *
     * @param filePath the path/key of the file
     * @return true if deleted successfully
     */
    boolean delete(String filePath);

    /**
     * Check if a file exists in storage
     *
     * @param filePath the path/key of the file
     * @return true if file exists
     */
    boolean exists(String filePath);

    /**
     * Get a public URL for the file
     *
     * @param filePath the path/key of the file
     * @return public URL string
     */
    String getPublicUrl(String filePath);

    /**
     * Generate a presigned URL for temporary access
     *
     * @param filePath the path/key of the file
     * @param duration how long the URL should be valid
     * @return presigned URL string
     */
    String getPresignedUrl(String filePath, Duration duration);

    /**
     * Generate a presigned URL for upload
     *
     * @param filePath    the path/key where file will be uploaded
     * @param contentType expected content type
     * @param duration    how long the URL should be valid
     * @return presigned upload URL string
     */
    String getPresignedUploadUrl(String filePath, String contentType, Duration duration);
}
