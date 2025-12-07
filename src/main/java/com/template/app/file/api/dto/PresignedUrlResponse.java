package com.template.app.file.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    private String presignedUrl;
    private String filePath;
    private String storedFilename;
    private long expiresInSeconds;
}
