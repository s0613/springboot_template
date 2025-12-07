package com.template.app.common.controller;

import com.template.app.common.dto.ApiResponse;
import com.template.app.common.dto.FileUploadResponse;
import com.template.app.common.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v2/files")
@RequiredArgsConstructor
@ConditionalOnBean(S3Service.class)
@Tag(name = "파일 관리", description = "S3 파일 업로드/관리 API입니다. 문제 생성 시 필요한 이미지를 먼저 업로드하고 반환된 URL을 사용합니다.")
public class FileController {

    private final S3Service s3Service;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "audio/mpeg",
            "audio/wav"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드",
               description = "단일 파일을 S3에 업로드합니다. " +
                       "지원 형식: JPEG, PNG, GIF, WebP (이미지), MP3, WAV (오디오). " +
                       "최대 크기: 10MB. " +
                       "반환: 업로드된 파일의 S3 URL. " +
                       "용도: 문제 생성 전 이미지/오디오를 업로드하고 반환된 URL을 문제 데이터에 사용합니다.")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "questions") String folder) {

        validateFile(file);

        String fileName = generateFileName(folder, file.getOriginalFilename());

        try {
            String fileUrl = s3Service.uploadFile(fileName, file.getBytes(), file.getContentType());

            FileUploadResponse response = FileUploadResponse.builder()
                    .url(fileUrl)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "파일이 업로드되었습니다"));

        } catch (IOException e) {
            log.error("Failed to read file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 읽기에 실패했습니다: " + file.getOriginalFilename());
        }
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 일괄 업로드",
               description = "여러 파일을 한 번에 S3에 업로드합니다. " +
                       "최대 개수: 20개. " +
                       "지원 형식: JPEG, PNG, GIF, WebP (이미지), MP3, WAV (오디오). " +
                       "각 파일 최대 크기: 10MB. " +
                       "반환: 업로드된 모든 파일의 S3 URL 목록. " +
                       "용도: 문제집 생성 시 필요한 이미지들을 한 번에 업로드합니다.")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "questions") String folder) {

        if (files.size() > 20) {
            throw new IllegalArgumentException("한 번에 최대 20개 파일만 업로드 가능합니다");
        }

        List<FileUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file);

            String fileName = generateFileName(folder, file.getOriginalFilename());

            try {
                String fileUrl = s3Service.uploadFile(fileName, file.getBytes(), file.getContentType());

                FileUploadResponse response = FileUploadResponse.builder()
                        .url(fileUrl)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .size(file.getSize())
                        .build();

                responses.add(response);

            } catch (IOException e) {
                log.error("Failed to read file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("파일 읽기에 실패했습니다: " + file.getOriginalFilename());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses, files.size() + "개 파일이 업로드되었습니다"));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + contentType);
        }
    }

    private String generateFileName(String folder, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }
}
