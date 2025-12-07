package com.template.app.file.api.dto;

import com.template.app.file.domain.entity.FileMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private Long id;
    private String originalFilename;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private FileMetadata.StorageType storageType;
    private FileMetadata.FileCategory fileCategory;
    private LocalDateTime createdAt;

    public static FileResponse from(FileMetadata metadata) {
        return FileResponse.builder()
                .id(metadata.getId())
                .originalFilename(metadata.getOriginalFilename())
                .fileUrl(metadata.getFileUrl())
                .contentType(metadata.getContentType())
                .fileSize(metadata.getFileSize())
                .storageType(metadata.getStorageType())
                .fileCategory(metadata.getFileCategory())
                .createdAt(metadata.getCreatedAt())
                .build();
    }
}
