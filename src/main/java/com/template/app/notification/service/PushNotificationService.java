package com.template.app.notification.service;

import com.template.app.notification.entity.PushToken;
import com.template.app.notification.repository.PushTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending push notifications via AWS SNS.
 * Supports both Android (GCM/FCM) and iOS (APNS) platforms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(SnsClient.class)
public class PushNotificationService {

    private final SnsClient snsClient;
    private final PushTokenRepository pushTokenRepository;
    private final ObjectMapper objectMapper;
    private DeadLetterQueueService dlqService; // Lazy injection to avoid circular dependency

    @Value("${aws.sns.enabled:false}")
    private boolean snsEnabled;

    @Value("${aws.sns.platform-application-arn.android:}")
    private String androidPlatformArn;

    @Value("${aws.sns.platform-application-arn.ios:}")
    private String iosPlatformArn;

    /**
     * Set DLQ service (for lazy injection to avoid circular dependency).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDlqService(DeadLetterQueueService dlqService) {
        this.dlqService = dlqService;
    }

    /**
     * Send push notification to all devices registered for a user.
     *
     * @param userId  Target user ID
     * @param title   Notification title
     * @param message Notification message
     * @param data    Additional data payload (optional)
     */
    @CircuitBreaker(name = "sns-service", fallbackMethod = "sendPushFallback")
    public void sendPush(Long userId, String title, String message, Map<String, String> data) {
        if (!snsEnabled) {
            log.warn("SNS is disabled. Skipping push notification for user {}", userId);
            return;
        }

        List<PushToken> tokens = pushTokenRepository.findByUserIdAndEnabledTrue(userId);

        if (tokens.isEmpty()) {
            log.info("No enabled push tokens found for user {}", userId);
            return;
        }

        log.info("Sending push notification to {} devices for user {}", tokens.size(), userId);

        for (PushToken token : tokens) {
            try {
                sendToDevice(token, title, message, data);
            } catch (Exception e) {
                log.error("Failed to send push to token {} (user {})", token.getId(), userId, e);

                // Add to DLQ for retry
                if (dlqService != null) {
                    String combinedMessage = title + ": " + message;
                    dlqService.addToQueue("PUSH", userId, token.getDeviceToken(), combinedMessage, e.getMessage());
                }

                // Continue with other tokens even if one fails
            }
        }
    }

    /**
     * Send push notification to a specific device.
     */
    private void sendToDevice(PushToken token, String title, String message, Map<String, String> data) {
        try {
            // Get or create SNS endpoint ARN for this device
            String endpointArn = getOrCreateEndpoint(token);

            // Create platform-specific message
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", message);
            if (data != null && !data.isEmpty()) {
                notification.put("data", data);
            }

            String messageJson = createPlatformMessage(token.getPlatform(), notification);

            // Publish to SNS
            PublishRequest publishRequest = PublishRequest.builder()
                .targetArn(endpointArn)
                .message(messageJson)
                .messageStructure("json")
                .build();

            PublishResponse response = snsClient.publish(publishRequest);

            log.info("Push notification sent successfully to user {} on {} (messageId: {})",
                token.getUserId(), token.getPlatform(), response.messageId());

        } catch (EndpointDisabledException e) {
            log.warn("Endpoint disabled for token {}, marking as disabled", token.getId());
            token.setEnabled(false);
            pushTokenRepository.save(token);
        } catch (InvalidParameterException e) {
            log.error("Invalid SNS parameter for token {}: {}", token.getId(), e.getMessage());
            throw new RuntimeException("Invalid SNS configuration", e);
        } catch (Exception e) {
            log.error("Failed to send push notification to token {}", token.getId(), e);
            throw new RuntimeException("Push notification failed", e);
        }
    }

    /**
     * Get existing endpoint ARN or create a new one for this device.
     */
    private String getOrCreateEndpoint(PushToken token) {
        // Return existing endpoint if available
        if (token.getEndpointArn() != null && !token.getEndpointArn().isEmpty()) {
            return token.getEndpointArn();
        }

        // Create new endpoint
        String platformArn = token.getPlatform().equals("ANDROID")
            ? androidPlatformArn : iosPlatformArn;

        if (platformArn == null || platformArn.isEmpty()) {
            throw new IllegalStateException(
                "Platform ARN not configured for " + token.getPlatform());
        }

        try {
            CreatePlatformEndpointRequest request = CreatePlatformEndpointRequest.builder()
                .platformApplicationArn(platformArn)
                .token(token.getDeviceToken())
                .build();

            CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(request);
            String endpointArn = response.endpointArn();

            log.info("Created SNS endpoint for token {} (user {}): {}",
                token.getId(), token.getUserId(), endpointArn);

            // Save endpoint ARN for future use
            token.setEndpointArn(endpointArn);
            pushTokenRepository.save(token);

            return endpointArn;

        } catch (Exception e) {
            log.error("Failed to create SNS endpoint for token {}", token.getId(), e);
            throw new RuntimeException("Failed to create SNS endpoint", e);
        }
    }

    /**
     * Create platform-specific message payload.
     * Different formats for Android (GCM) and iOS (APNS).
     */
    private String createPlatformMessage(String platform, Map<String, Object> notification) {
        try {
            Map<String, String> messages = new HashMap<>();

            if (platform.equals("ANDROID")) {
                // GCM/FCM format
                Map<String, Object> gcmMessage = new HashMap<>();
                gcmMessage.put("notification", notification);
                gcmMessage.put("priority", "high");

                // Wrap in data payload for GCM
                Map<String, Object> gcmData = new HashMap<>();
                gcmData.put("data", gcmMessage);

                messages.put("GCM", objectMapper.writeValueAsString(gcmData));

            } else {
                // APNS format
                Map<String, Object> alert = new HashMap<>();
                alert.put("title", notification.get("title"));
                alert.put("body", notification.get("body"));

                Map<String, Object> aps = new HashMap<>();
                aps.put("alert", alert);
                aps.put("sound", "default");

                Map<String, Object> apnsMessage = new HashMap<>();
                apnsMessage.put("aps", aps);

                // Add custom data if present
                if (notification.containsKey("data")) {
                    apnsMessage.put("data", notification.get("data"));
                }

                messages.put("APNS", objectMapper.writeValueAsString(apnsMessage));
                // Also add APNS_SANDBOX for development
                messages.put("APNS_SANDBOX", objectMapper.writeValueAsString(apnsMessage));
            }

            // Add default message for other platforms
            messages.put("default", notification.get("body").toString());

            return objectMapper.writeValueAsString(messages);

        } catch (Exception e) {
            log.error("Failed to create platform message", e);
            throw new RuntimeException("Failed to create platform message", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open.
     */
    @SuppressWarnings("unused")
    private void sendPushFallback(Long userId, String title, String message,
                                   Map<String, String> data, Throwable t) {
        log.error("Push notification circuit breaker fallback for user {}: {}",
            userId, t.getMessage());
        log.info("Push notification would have been sent: title='{}', message='{}'",
            title, message);

        // Add to DLQ for retry
        if (dlqService != null) {
            String combinedMessage = title + ": " + message;
            dlqService.addToQueue("PUSH", userId, "circuit-breaker-fallback", combinedMessage, t.getMessage());
        }
    }

    /**
     * Register a new device token for push notifications.
     *
     * @param userId      User ID
     * @param deviceToken Device token from FCM/APNS
     * @param platform    Platform type (ANDROID or IOS)
     * @return Saved PushToken entity
     */
    public PushToken registerDeviceToken(Long userId, String deviceToken, String platform) {
        log.info("Registering device token for user {} on platform {}", userId, platform);

        // Validate platform
        if (!platform.equals("ANDROID") && !platform.equals("IOS")) {
            throw new IllegalArgumentException("Invalid platform: " + platform);
        }

        // Check if token already exists
        return pushTokenRepository.findByDeviceToken(deviceToken)
            .map(existingToken -> {
                log.info("Device token already exists, updating user ID to {}", userId);
                existingToken.setUserId(userId);
                existingToken.setEnabled(true);
                return pushTokenRepository.save(existingToken);
            })
            .orElseGet(() -> {
                // Create new token
                PushToken newToken = new PushToken();
                newToken.setUserId(userId);
                newToken.setDeviceToken(deviceToken);
                newToken.setPlatform(platform);
                newToken.setEnabled(true);
                return pushTokenRepository.save(newToken);
            });
    }

    /**
     * Disable push notifications for a device token.
     */
    public void disableDeviceToken(String deviceToken) {
        pushTokenRepository.findByDeviceToken(deviceToken)
            .ifPresent(token -> {
                log.info("Disabling push token {}", token.getId());
                token.setEnabled(false);
                pushTokenRepository.save(token);
            });
    }
}
