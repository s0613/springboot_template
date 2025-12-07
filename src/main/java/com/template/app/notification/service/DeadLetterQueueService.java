package com.template.app.notification.service;

import com.template.app.common.integration.sms.SmsService;
import com.template.app.notification.entity.FailedNotification;
import com.template.app.notification.repository.FailedNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing failed notifications and retry logic.
 * Implements a Dead Letter Queue (DLQ) pattern with exponential backoff.
 * Only enabled when notification services are available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.dlq.enabled", havingValue = "true", matchIfMissing = false)
public class DeadLetterQueueService {

    private final FailedNotificationRepository repository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;

    /**
     * Add a failed notification to the DLQ for retry.
     *
     * @param type    Notification type (EMAIL, SMS, PUSH)
     * @param userId  Recipient user ID (can be null for email-only notifications)
     * @param address Recipient address (email/phone/deviceToken)
     * @param content Message content
     * @param error   Error message from the failure
     */
    @Transactional
    public void addToQueue(String type, Long userId, String address, String content, String error) {
        FailedNotification failed = new FailedNotification();
        failed.setNotificationType(type);
        failed.setRecipientUserId(userId);
        failed.setRecipientAddress(address);
        failed.setMessageContent(content);
        failed.setErrorMessage(error);
        failed.setStatus("PENDING");
        failed.setNextRetryAt(calculateNextRetry(0));

        repository.save(failed);
        log.warn("Added failed {} notification to DLQ for user {}: {}", type, userId, error);
    }

    /**
     * Process pending retry attempts.
     * Runs every 5 minutes to check for notifications ready for retry.
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void processRetries() {
        List<FailedNotification> pending = repository.findPendingRetries(LocalDateTime.now());

        if (pending.isEmpty()) {
            log.debug("No pending notification retries found");
            return;
        }

        log.info("Processing {} pending notification retries", pending.size());

        for (FailedNotification notification : pending) {
            try {
                retryNotification(notification);
            } catch (Exception e) {
                log.error("Error processing retry for notification {}", notification.getId(), e);
            }
        }
    }

    /**
     * Retry a failed notification.
     */
    private void retryNotification(FailedNotification notification) {
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setLastRetryAt(LocalDateTime.now());
        notification.setStatus("RETRYING");

        try {
            boolean success = switch (notification.getNotificationType()) {
                case "EMAIL" -> retryEmail(notification);
                case "SMS" -> retrySms(notification);
                case "PUSH" -> retryPush(notification);
                default -> {
                    log.error("Unknown notification type: {}", notification.getNotificationType());
                    yield false;
                }
            };

            if (success) {
                notification.setStatus("SUCCEEDED");
                notification.setSucceededAt(LocalDateTime.now());
                log.info("Successfully retried {} notification {} after {} attempts",
                    notification.getNotificationType(), notification.getId(), notification.getRetryCount());
            } else {
                handleRetryFailure(notification);
            }
        } catch (Exception e) {
            log.error("Retry failed for notification {}", notification.getId(), e);
            notification.setErrorMessage(e.getMessage());
            handleRetryFailure(notification);
        }

        repository.save(notification);
    }

    /**
     * Handle retry failure and determine next steps.
     */
    private void handleRetryFailure(FailedNotification notification) {
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            notification.setStatus("FAILED");
            log.error("Notification {} permanently failed after {} retries (type: {}, recipient: {})",
                notification.getId(), notification.getRetryCount(),
                notification.getNotificationType(), notification.getRecipientAddress());
        } else {
            notification.setStatus("PENDING");
            notification.setNextRetryAt(calculateNextRetry(notification.getRetryCount()));
            log.warn("Notification {} retry failed, will retry again at {}",
                notification.getId(), notification.getNextRetryAt());
        }
    }

    /**
     * Calculate next retry time with exponential backoff.
     * Backoff pattern: 5min, 15min, 45min (3^n * 5 minutes)
     *
     * @param retryCount Current retry count
     * @return Next retry timestamp
     */
    private LocalDateTime calculateNextRetry(int retryCount) {
        // Exponential backoff: 5min, 15min, 45min
        long delayMinutes = (long) Math.pow(3, retryCount) * 5;
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * Retry sending an email notification.
     */
    private boolean retryEmail(FailedNotification notification) {
        try {
            // Use a simplified email sending for retries
            emailService.sendEmail(
                notification.getRecipientAddress(),
                "Retry: Alert Notification",
                notification.getMessageContent()
            );
            return true;
        } catch (Exception e) {
            log.debug("Email retry failed for notification {}: {}",
                notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Retry sending an SMS notification.
     */
    private boolean retrySms(FailedNotification notification) {
        try {
            return smsService.sendSms(
                notification.getRecipientAddress(),
                notification.getMessageContent()
            );
        } catch (Exception e) {
            log.debug("SMS retry failed for notification {}: {}",
                notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Retry sending a push notification.
     */
    private boolean retryPush(FailedNotification notification) {
        try {
            if (notification.getRecipientUserId() == null) {
                log.error("Cannot retry push notification without user ID");
                return false;
            }

            pushNotificationService.sendPush(
                notification.getRecipientUserId(),
                "Alert Notification",
                notification.getMessageContent(),
                null
            );
            return true;
        } catch (Exception e) {
            log.debug("Push retry failed for notification {}: {}",
                notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Get recent failures for monitoring.
     *
     * @param hours Number of hours to look back
     * @return List of recent permanently failed notifications
     */
    public List<FailedNotification> getRecentFailures(int hours) {
        return repository.findRecentFailures(LocalDateTime.now().minusHours(hours));
    }

    /**
     * Get DLQ statistics for monitoring.
     *
     * @param hours Number of hours to look back
     * @return Map of statistics
     */
    public DlqStats getStats(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        long pending = repository.countByStatusAndCreatedAtAfter("PENDING", since);
        long failed = repository.countByStatusAndCreatedAtAfter("FAILED", since);
        long succeeded = repository.countByStatusAndCreatedAtAfter("SUCCEEDED", since);
        long retrying = repository.countByStatusAndCreatedAtAfter("RETRYING", since);

        return new DlqStats(pending, failed, succeeded, retrying);
    }

    /**
     * Statistics holder for DLQ monitoring.
     */
    public record DlqStats(long pending, long failed, long succeeded, long retrying) {
        public long total() {
            return pending + failed + succeeded + retrying;
        }
    }
}
