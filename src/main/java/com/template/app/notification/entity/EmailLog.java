package com.template.app.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for logging all email send attempts.
 * Tracks success and failure for monitoring and debugging.
 */
@Entity
@Table(name = "email_logs", indexes = {
    @Index(name = "idx_email_logs_recipient_email", columnList = "recipient_email"),
    @Index(name = "idx_email_logs_status", columnList = "status"),
    @Index(name = "idx_email_logs_sent_at", columnList = "sent_at"),
    @Index(name = "idx_email_logs_template_name", columnList = "template_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "template_name", nullable = false, length = 50)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "message_id", length = 255)
    private String messageId;  // SES message ID

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * Email send status
     */
    public enum EmailStatus {
        SENT,      // Successfully sent
        FAILED,    // Failed to send
        PENDING    // Queued for sending
    }

    /**
     * Factory method for successful email log
     */
    public static EmailLog success(String recipientEmail, String subject, String templateName, String messageId) {
        return EmailLog.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .templateName(templateName)
                .status(EmailStatus.SENT)
                .messageId(messageId)
                .build();
    }

    /**
     * Factory method for failed email log
     */
    public static EmailLog failure(String recipientEmail, String subject, String templateName, String errorMessage) {
        return EmailLog.builder()
                .recipientEmail(recipientEmail)
                .subject(subject)
                .templateName(templateName)
                .status(EmailStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
