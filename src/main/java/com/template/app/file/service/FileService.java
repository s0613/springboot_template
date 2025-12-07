package com.template.app.file.service;

import com.template.app.file.domain.entity.FileMetadata;
import com.template.app.file.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorageService fileStorageService;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize; // 10MB default

    @Value("${file.upload.allowed-types:image/jpeg,image/png,image/gif,application/pdf}")
    private String allowedTypes;

    /**
     * Upload a file and save metadata
     */
    @Transactional
    public FileMetadata uploadFile(MultipartFile file, String path, FileMetadata.FileCategory category, Long uploaderId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateStoredFilename(originalFilename);
        String filePath = fileStorageService.upload(file, path, storedFilename);
        String fileUrl = fileStorageService.getPublicUrl(filePath);

        FileMetadata metadata = FileMetadata.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileUrl(fileUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storageType(s3Enabled ? FileMetadata.StorageType.S3 : FileMetadata.StorageType.LOCAL)
                .fileCategory(category)
                .uploaderId(uploaderId)
                .isPublic(false)
                .build();

        return fileMetadataRepository.save(metadata);
    }

    /**
     * Upload a file with reference to another entity
     */
    @Transactional
    public FileMetadata uploadFileWithReference(
            MultipartFile file,
            String path,
            FileMetadata.FileCategory category,
            Long uploaderId,
            String referenceType,
            Long referenceId
    ) {
        FileMetadata metadata = uploadFile(file, path, category, uploaderId);
        metadata.setReferenceType(referenceType);
        metadata.setReferenceId(referenceId);
        return fileMetadataRepository.save(metadata);
    }

    /**
     * Get file metadata by ID
     */
    @Transactional(readOnly = true)
    public FileMetadata getFileById(Long id) {
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found: " + id));
    }

    /**
     * Get presigned download URL
     */
    public String getPresignedDownloadUrl(Long fileId, Duration duration) {
        FileMetadata metadata = getFileById(fileId);
        return fileStorageService.getPresignedUrl(metadata.getFilePath(), duration);
    }

    /**
     * Get presigned upload URL for direct upload
     */
    public PresignedUploadInfo getPresignedUploadUrl(String path, String filename, String contentType, Duration duration) {
        String storedFilename = generateStoredFilename(filename);
        String filePath = (path != null && !path.isEmpty()) ? path + "/" + storedFilename : storedFilename;
        String presignedUrl = fileStorageService.getPresignedUploadUrl(filePath, contentType, duration);

        return new PresignedUploadInfo(presignedUrl, filePath, storedFilename);
    }

    /**
     * Confirm a direct upload (after client uploads via presigned URL)
     */
    @Transactional
    public FileMetadata confirmDirectUpload(
            String filePath,
            String storedFilename,
            String originalFilename,
            String contentType,
            Long fileSize,
            FileMetadata.FileCategory category,
            Long uploaderId
    ) {
        if (!fileStorageService.exists(filePath)) {
            throw new RuntimeException("File not found at path: " + filePath);
        }

        FileMetadata metadata = FileMetadata.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath)
                .fileUrl(fileStorageService.getPublicUrl(filePath))
                .contentType(contentType)
                .fileSize(fileSize)
                .storageType(s3Enabled ? FileMetadata.StorageType.S3 : FileMetadata.StorageType.LOCAL)
                .fileCategory(category)
                .uploaderId(uploaderId)
                .isPublic(false)
                .build();

        return fileMetadataRepository.save(metadata);
    }

    /**
     * Delete a file (soft delete)
     */
    @Transactional
    public void deleteFile(Long fileId) {
        FileMetadata metadata = getFileById(fileId);
        metadata.markAsDeleted();
        fileMetadataRepository.save(metadata);
        log.info("File soft deleted: {}", fileId);
    }

    /**
     * Permanently delete a file
     */
    @Transactional
    public void permanentlyDeleteFile(Long fileId) {
        FileMetadata metadata = getFileById(fileId);
        fileStorageService.delete(metadata.getFilePath());
        fileMetadataRepository.delete(metadata);
        log.info("File permanently deleted: {}", fileId);
    }

    /**
     * Get files by uploader
     */
    @Transactional(readOnly = true)
    public Page<FileMetadata> getFilesByUploader(Long uploaderId, Pageable pageable) {
        return fileMetadataRepository.findByUploaderIdAndNotDeleted(uploaderId, pageable);
    }

    /**
     * Get files by reference
     */
    @Transactional(readOnly = true)
    public List<FileMetadata> getFilesByReference(String referenceType, Long referenceId) {
        return fileMetadataRepository.findByReference(referenceType, referenceId);
    }

    /**
     * Get total storage used by uploader
     */
    @Transactional(readOnly = true)
    public Long getTotalStorageUsed(Long uploaderId) {
        Long total = fileMetadataRepository.getTotalFileSizeByUploader(uploaderId);
        return total != null ? total : 0L;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds limit: " + maxFileSize);
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("File type not allowed: " + contentType);
        }
    }

    private String generateStoredFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public record PresignedUploadInfo(String presignedUrl, String filePath, String storedFilename) {}
}
