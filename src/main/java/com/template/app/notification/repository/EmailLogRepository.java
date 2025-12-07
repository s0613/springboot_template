package com.template.app.notification.repository;

import com.template.app.notification.entity.EmailLog;
import com.template.app.notification.entity.EmailLog.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EmailLog entity.
 * Provides queries for email statistics and monitoring.
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /**
     * Find email logs by recipient email within a date range
     */
    List<EmailLog> findByRecipientEmailAndSentAtBetween(
            String recipientEmail,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find email logs by status
     */
    List<EmailLog> findByStatus(EmailStatus status);

    /**
     * Find failed email logs for retry
     */
    @Query("SELECT e FROM EmailLog e WHERE e.status = 'FAILED' AND e.sentAt > :since ORDER BY e.sentAt DESC")
    List<EmailLog> findRecentFailures(@Param("since") LocalDateTime since);

    /**
     * Count emails sent to a recipient in the last N hours (for rate limiting)
     */
    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.recipientEmail = :email AND e.sentAt > :since")
    long countRecentEmailsToRecipient(@Param("email") String email, @Param("since") LocalDateTime since);

    /**
     * Find email logs by template name
     */
    List<EmailLog> findByTemplateName(String templateName);

    /**
     * Get success rate for email sending
     */
    @Query("SELECT " +
           "(CAST(COUNT(CASE WHEN e.status = 'SENT' THEN 1 END) AS double) / CAST(COUNT(e) AS double)) * 100 " +
           "FROM EmailLog e WHERE e.sentAt > :since")
    Double getSuccessRatePercentage(@Param("since") LocalDateTime since);
}
