package com.template.app.audit.api.controller;

import com.template.app.audit.domain.entity.AuditLog;
import com.template.app.audit.service.AuditService;
import com.template.app.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log APIs (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit history for an entity")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        List<AuditLog> history = auditService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/actor/{actorId}")
    @Operation(summary = "Get audit logs by actor")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogsByActor(
            @PathVariable Long actorId,
            Pageable pageable
    ) {
        Page<AuditLog> logs = auditService.getLogsByActor(actorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> searchLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditLog.AuditAction action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<AuditLog> logs = auditService.searchLogs(entityType, action, actorId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stats/{action}")
    @Operation(summary = "Get action count since a date")
    public ResponseEntity<ApiResponse<Long>> getActionCount(
            @PathVariable AuditLog.AuditAction action,
            @RequestParam(defaultValue = "7") int days
    ) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Long count = auditService.getActionCountSince(action, since);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Clean up old audit logs")
    public ResponseEntity<ApiResponse<Void>> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int retentionDays
    ) {
        auditService.cleanupOldLogs(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
