package com.template.app.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a device push notification token.
 * Stores FCM/APNS tokens for sending push notifications to mobile devices.
 */
@Entity
@Table(name = "push_tokens")
@Getter
@Setter
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID who owns this device token
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Device token from FCM (Android) or APNS (iOS)
     */
    @Column(nullable = false, unique = true, length = 500)
    private String deviceToken;

    /**
     * Platform type: ANDROID or IOS
     */
    @Column(nullable = false, length = 20)
    private String platform;

    /**
     * SNS endpoint ARN (created when device is registered)
     */
    @Column(length = 500)
    private String endpointArn;

    /**
     * Whether this token is enabled for push notifications
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
