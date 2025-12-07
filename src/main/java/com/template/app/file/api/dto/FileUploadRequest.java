package com.template.app.file.api.dto;

import com.template.app.file.domain.entity.FileMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    @NotNull(message = "File category is required")
    private FileMetadata.FileCategory category;

    private String path;

    private String referenceType;

    private Long referenceId;
}
