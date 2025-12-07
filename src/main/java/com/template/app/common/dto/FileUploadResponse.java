package com.template.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파일 업로드 응답")
public class FileUploadResponse {

    @Schema(description = "업로드된 파일의 S3 URL", example = "https://cogmo-annyeong-storage.s3.ap-northeast-2.amazonaws.com/questions/abc123.png")
    private String url;

    @Schema(description = "원본 파일명", example = "memory_scene.png")
    private String fileName;

    @Schema(description = "파일 MIME 타입", example = "image/png")
    private String contentType;

    @Schema(description = "파일 크기 (bytes)", example = "102400")
    private Long size;
}
