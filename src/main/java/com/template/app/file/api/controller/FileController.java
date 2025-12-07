package com.template.app.file.api.controller;

import com.template.app.common.dto.ApiResponse;
import com.template.app.file.api.dto.FileResponse;
import com.template.app.file.api.dto.PresignedUrlResponse;
import com.template.app.file.domain.entity.FileMetadata;
import com.template.app.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "File upload and management APIs")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file")
    public ResponseEntity<ApiResponse<FileResponse>> uploadFile(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "OTHER") FileMetadata.FileCategory category,
            @RequestParam(value = "path", required = false) String path
    ) {
        Long userId = Long.parseLong(userIdStr);
        FileMetadata metadata = fileService.uploadFile(file, path, category, userId);
        return ResponseEntity.ok(ApiResponse.success(FileResponse.from(metadata)));
    }

    @PostMapping(value = "/with-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file with entity reference")
    public ResponseEntity<ApiResponse<FileResponse>> uploadFileWithReference(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "OTHER") FileMetadata.FileCategory category,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam("referenceType") String referenceType,
            @RequestParam("referenceId") Long referenceId
    ) {
        Long userId = Long.parseLong(userIdStr);
        FileMetadata metadata = fileService.uploadFileWithReference(
                file, path, category, userId, referenceType, referenceId
        );
        return ResponseEntity.ok(ApiResponse.success(FileResponse.from(metadata)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file metadata")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(@PathVariable Long id) {
        FileMetadata metadata = fileService.getFileById(id);
        return ResponseEntity.ok(ApiResponse.success(FileResponse.from(metadata)));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get presigned download URL")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getDownloadUrl(
            @PathVariable Long id,
            @RequestParam(value = "expiresInMinutes", defaultValue = "60") int expiresInMinutes
    ) {
        Duration duration = Duration.ofMinutes(expiresInMinutes);
        String presignedUrl = fileService.getPresignedDownloadUrl(id, duration);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .expiresInSeconds(duration.toSeconds())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/presigned-upload-url")
    @Operation(summary = "Get presigned upload URL for direct upload")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "expiresInMinutes", defaultValue = "15") int expiresInMinutes
    ) {
        Duration duration = Duration.ofMinutes(expiresInMinutes);
        FileService.PresignedUploadInfo uploadInfo = fileService.getPresignedUploadUrl(
                path, filename, contentType, duration
        );

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .presignedUrl(uploadInfo.presignedUrl())
                .filePath(uploadInfo.filePath())
                .storedFilename(uploadInfo.storedFilename())
                .expiresInSeconds(duration.toSeconds())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/confirm-upload")
    @Operation(summary = "Confirm a direct upload")
    public ResponseEntity<ApiResponse<FileResponse>> confirmDirectUpload(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam("filePath") String filePath,
            @RequestParam("storedFilename") String storedFilename,
            @RequestParam("originalFilename") String originalFilename,
            @RequestParam("contentType") String contentType,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam(value = "category", defaultValue = "OTHER") FileMetadata.FileCategory category
    ) {
        Long userId = Long.parseLong(userIdStr);
        FileMetadata metadata = fileService.confirmDirectUpload(
                filePath, storedFilename, originalFilename, contentType, fileSize, category, userId
        );
        return ResponseEntity.ok(ApiResponse.success(FileResponse.from(metadata)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my uploaded files")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getMyFiles(
            @AuthenticationPrincipal String userIdStr,
            Pageable pageable
    ) {
        Long userId = Long.parseLong(userIdStr);
        Page<FileResponse> files = fileService.getFilesByUploader(userId, pageable)
                .map(FileResponse::from);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @GetMapping("/by-reference")
    @Operation(summary = "Get files by entity reference")
    public ResponseEntity<ApiResponse<List<FileResponse>>> getFilesByReference(
            @RequestParam("referenceType") String referenceType,
            @RequestParam("referenceId") Long referenceId
    ) {
        List<FileResponse> files = fileService.getFilesByReference(referenceType, referenceId)
                .stream()
                .map(FileResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Permanently delete a file")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteFile(@PathVariable Long id) {
        fileService.permanentlyDeleteFile(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/storage-usage")
    @Operation(summary = "Get my storage usage")
    public ResponseEntity<ApiResponse<Long>> getStorageUsage(@AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        Long totalBytes = fileService.getTotalStorageUsed(userId);
        return ResponseEntity.ok(ApiResponse.success(totalBytes));
    }
}
