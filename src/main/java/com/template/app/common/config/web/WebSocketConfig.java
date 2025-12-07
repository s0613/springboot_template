package com.template.app.common.config.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications.
 * Uses STOMP over WebSocket with SockJS fallback.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.oauth2.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Create TaskScheduler for heartbeat
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("wsSockJsScheduler-");
        taskScheduler.initialize();

        // Enable simple in-memory broker for destinations with /topic and /queue prefixes
        // NOTE: SimpleBroker is for single-instance deployments only.
        // For multi-instance/distributed deployments, switch to external broker (Redis, RabbitMQ)
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{5000, 5000}) // 5-second heartbeat interval
                .setTaskScheduler(taskScheduler);

        // Set application destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for sending messages to specific users
        config.setUserDestinationPrefix("/user");

        log.info("Configured WebSocket message broker with prefixes: /topic, /queue, /app, /user and 5s heartbeat");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        // SECURITY: Restrict origins to match CORS policy in SecurityConfig
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        // setAllowedOriginPatterns expects varargs, so use array spread operator
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)  // varargs expects String... not String[]
                .withSockJS();

        log.info("Registered STOMP endpoint at /ws with SockJS fallback for origins: {}", String.join(", ", origins));
    }
}
