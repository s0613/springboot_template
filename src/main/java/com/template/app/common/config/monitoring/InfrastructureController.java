package com.template.app.common.config.monitoring;

import com.template.app.notification.repository.FailedNotificationRepository;
import com.template.app.notification.service.DeadLetterQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for infrastructure monitoring and operations.
 * Provides endpoints for DLQ stats, circuit breaker status, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/infrastructure")
@RequiredArgsConstructor
@ConditionalOnBean(DeadLetterQueueService.class)
@Tag(name = "Infrastructure", description = "Infrastructure monitoring and operations")
public class InfrastructureController {

    private final FailedNotificationRepository failedNotificationRepository;
    private final DeadLetterQueueService deadLetterQueueService;

    /**
     * Get Dead Letter Queue statistics.
     * Shows pending, failed, and succeeded notification counts.
     *
     * @param hours Number of hours to look back (default: 24)
     * @return DLQ statistics
     */
    @GetMapping("/dlq/stats")
    @Operation(
        summary = "Get DLQ statistics",
        description = "Returns statistics about failed notifications in the Dead Letter Queue"
    )
    public ResponseEntity<Map<String, Object>> getDlqStats(
            @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);

            // Get counts by status
            long pending = failedNotificationRepository.countByStatusAndCreatedAtAfter("PENDING", since);
            long failed = failedNotificationRepository.countByStatusAndCreatedAtAfter("FAILED", since);
            long succeeded = failedNotificationRepository.countByStatusAndCreatedAtAfter("SUCCEEDED", since);
            long retrying = failedNotificationRepository.countByStatusAndCreatedAtAfter("RETRYING", since);

            // Get detailed stats from service
            DeadLetterQueueService.DlqStats stats = deadLetterQueueService.getStats(hours);

            Map<String, Object> response = new HashMap<>();
            response.put("timeRange", hours + " hours");
            response.put("since", since);
            response.put("now", LocalDateTime.now());

            Map<String, Object> counts = new HashMap<>();
            counts.put("pending", pending);
            counts.put("retrying", retrying);
            counts.put("failed", failed);
            counts.put("succeeded", succeeded);
            counts.put("total", pending + retrying + failed + succeeded);
            response.put("counts", counts);

            // Calculate success rate
            long totalAttempts = failed + succeeded;
            double successRate = totalAttempts > 0 ? (double) succeeded / totalAttempts * 100 : 0.0;
            response.put("successRate", String.format("%.2f%%", successRate));

            // Health indicators
            Map<String, Object> health = new HashMap<>();
            health.put("status", failed > 10 ? "WARNING" : "OK");
            health.put("pendingRetries", pending);
            health.put("permanentFailures", failed);
            response.put("health", health);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get DLQ stats", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve DLQ statistics");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get recent permanently failed notifications.
     * Useful for debugging and manual intervention.
     *
     * @param hours Number of hours to look back (default: 24)
     * @return List of failed notifications
     */
    @GetMapping("/dlq/failures")
    @Operation(
        summary = "Get recent failures",
        description = "Returns list of permanently failed notifications for investigation"
    )
    public ResponseEntity<Map<String, Object>> getRecentFailures(
            @RequestParam(defaultValue = "24") int hours) {

        try {
            var failures = deadLetterQueueService.getRecentFailures(hours);

            Map<String, Object> response = new HashMap<>();
            response.put("timeRange", hours + " hours");
            response.put("count", failures.size());
            response.put("failures", failures.stream().map(f -> {
                Map<String, Object> failure = new HashMap<>();
                failure.put("id", f.getId());
                failure.put("type", f.getNotificationType());
                failure.put("recipient", f.getRecipientAddress());
                failure.put("retryCount", f.getRetryCount());
                failure.put("error", f.getErrorMessage());
                failure.put("createdAt", f.getCreatedAt());
                failure.put("lastRetryAt", f.getLastRetryAt());
                return failure;
            }).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get recent failures", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve failures");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get overall infrastructure health status.
     * Aggregates various infrastructure metrics.
     *
     * @return Infrastructure health status
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get infrastructure status",
        description = "Returns overall infrastructure health including DLQ, circuit breakers, etc."
    )
    public ResponseEntity<Map<String, Object>> getInfrastructureStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // DLQ health
            DeadLetterQueueService.DlqStats dlqStats = deadLetterQueueService.getStats(1);
            Map<String, Object> dlqHealth = new HashMap<>();
            dlqHealth.put("pending", dlqStats.pending());
            dlqHealth.put("failed", dlqStats.failed());
            dlqHealth.put("status", dlqStats.failed() > 10 ? "WARNING" : "OK");
            status.put("dlq", dlqHealth);

            // Overall status
            String overallStatus = "OK";
            if (dlqStats.failed() > 10) {
                overallStatus = "WARNING";
            }
            if (dlqStats.failed() > 50) {
                overallStatus = "CRITICAL";
            }

            status.put("overall", overallStatus);
            status.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Failed to get infrastructure status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve status");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
