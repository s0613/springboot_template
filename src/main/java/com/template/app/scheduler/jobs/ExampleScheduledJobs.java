package com.template.app.scheduler.jobs;

import com.template.app.scheduler.annotation.ScheduledWithLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Example scheduled jobs demonstrating @ScheduledWithLock usage.
 * These are disabled by default. Enable in application.yml with scheduling.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class ExampleScheduledJobs {

    /**
     * Example: Clean up expired tokens every hour.
     * Uses distributed lock to ensure only one instance runs this job.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @ScheduledWithLock(
            lockKey = "cleanup-expired-tokens",
            lockDurationSeconds = 600,
            jobGroup = "cleanup"
    )
    public Integer cleanupExpiredTokens() {
        log.info("Running cleanup-expired-tokens job");

        // TODO: Implement your cleanup logic here
        // Example:
        // int deleted = tokenRepository.deleteExpiredTokens();
        // return deleted;

        return 0;
    }

    /**
     * Example: Send daily digest emails.
     * Runs at 9 AM every day.
     */
    @Scheduled(cron = "0 0 9 * * *") // 9 AM daily
    @ScheduledWithLock(
            lockKey = "send-daily-digest",
            lockDurationSeconds = 1800, // 30 minutes
            jobGroup = "notification"
    )
    public Integer sendDailyDigest() {
        log.info("Running send-daily-digest job");

        // TODO: Implement your digest logic here
        // Example:
        // List<User> users = userRepository.findUsersWithDigestEnabled();
        // for (User user : users) {
        //     emailService.sendDailyDigest(user);
        // }
        // return users.size();

        return 0;
    }

    /**
     * Example: Sync data with external service.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @ScheduledWithLock(
            lockKey = "sync-external-data",
            lockDurationSeconds = 300,
            jobGroup = "sync"
    )
    public void syncExternalData() {
        log.info("Running sync-external-data job");

        // TODO: Implement your sync logic here
    }

    /**
     * Example: Process failed notifications from DLQ.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @ScheduledWithLock(
            lockKey = "process-dlq",
            lockDurationSeconds = 300,
            jobGroup = "notification"
    )
    public Integer processDlq() {
        log.info("Running process-dlq job");

        // TODO: Implement DLQ processing logic here
        // This could call NotificationDlqService.processFailedNotifications()

        return 0;
    }
}
