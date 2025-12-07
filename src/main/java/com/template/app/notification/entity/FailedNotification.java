package com.template.app.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_notifications")
@Getter
@Setter
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String notificationType; // EMAIL, SMS, PUSH

    @Column(nullable = false)
    private Long recipientUserId;

    @Column(nullable = false)
    private String recipientAddress; // email/phone/deviceToken

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private Integer maxRetries = 3;

    private LocalDateTime nextRetryAt;

    @Column(nullable = false)
    private String status; // PENDING, RETRYING, FAILED, SUCCEEDED

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastRetryAt;
    private LocalDateTime succeededAt;
}
