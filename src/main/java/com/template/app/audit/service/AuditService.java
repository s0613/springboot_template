package com.template.app.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.app.audit.domain.entity.AuditLog;
import com.template.app.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an audit event synchronously
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(AuditLog.AuditAction action, String entityType, Long entityId, Long actorId, String description) {
        AuditLog auditLog = buildAuditLog(action, entityType, entityId, actorId, description);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log an audit event with old and new values
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logWithChanges(
            AuditLog.AuditAction action,
            String entityType,
            Long entityId,
            Long actorId,
            Object oldValue,
            Object newValue,
            String changedFields,
            String description
    ) {
        AuditLog auditLog = buildAuditLog(action, entityType, entityId, actorId, description);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLog.setChangedFields(changedFields);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Log an audit event asynchronously
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(AuditLog.AuditAction action, String entityType, Long entityId, Long actorId, String description) {
        try {
            AuditLog auditLog = buildAuditLog(action, entityType, entityId, actorId, description);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log asynchronously", e);
        }
    }

    /**
     * Log a create event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Long actorId, Object entity) {
        AuditLog auditLog = buildAuditLog(AuditLog.AuditAction.CREATE, entityType, entityId, actorId, "Created " + entityType);
        auditLog.setNewValue(toJson(entity));
        auditLogRepository.save(auditLog);
    }

    /**
     * Log an update event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Long actorId, Object oldEntity, Object newEntity, String changedFields) {
        AuditLog auditLog = buildAuditLog(AuditLog.AuditAction.UPDATE, entityType, entityId, actorId, "Updated " + entityType);
        auditLog.setOldValue(toJson(oldEntity));
        auditLog.setNewValue(toJson(newEntity));
        auditLog.setChangedFields(changedFields);
        auditLogRepository.save(auditLog);
    }

    /**
     * Log a delete event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId, Long actorId, Object entity) {
        AuditLog auditLog = buildAuditLog(AuditLog.AuditAction.DELETE, entityType, entityId, actorId, "Deleted " + entityType);
        auditLog.setOldValue(toJson(entity));
        auditLogRepository.save(auditLog);
    }

    /**
     * Log a login event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(Long userId, boolean success) {
        AuditLog auditLog = buildAuditLog(
                AuditLog.AuditAction.LOGIN,
                "User",
                userId,
                userId,
                success ? "Login successful" : "Login failed"
        );
        auditLogRepository.save(auditLog);
    }

    /**
     * Log a logout event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId) {
        AuditLog auditLog = buildAuditLog(AuditLog.AuditAction.LOGOUT, "User", userId, userId, "Logout");
        auditLogRepository.save(auditLog);
    }

    /**
     * Get audit history for an entity
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findEntityHistory(entityType, entityId);
    }

    /**
     * Get audit logs by actor
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByActor(Long actorId, Pageable pageable) {
        return auditLogRepository.findByActorId(actorId, pageable);
    }

    /**
     * Search audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(
            String entityType,
            AuditLog.AuditAction action,
            Long actorId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        return auditLogRepository.searchAuditLogs(entityType, action, actorId, startDate, endDate, pageable);
    }

    /**
     * Get action count since a date
     */
    @Transactional(readOnly = true)
    public Long getActionCountSince(AuditLog.AuditAction action, LocalDateTime since) {
        return auditLogRepository.countByActionSince(action, since);
    }

    /**
     * Clean up old audit logs
     */
    @Transactional
    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        auditLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up audit logs older than {} days", retentionDays);
    }

    private AuditLog buildAuditLog(AuditLog.AuditAction action, String entityType, Long entityId, Long actorId, String description) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actorId(actorId)
                .description(description);

        // Try to get request context
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                builder.actorIp(getClientIp(request));
                builder.actorUserAgent(request.getHeader("User-Agent"));
                builder.requestPath(request.getRequestURI());
                builder.requestMethod(request.getMethod());
            }
        } catch (Exception e) {
            log.debug("Could not get request context for audit log", e);
        }

        return builder.build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return object.toString();
        }
    }
}
