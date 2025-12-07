package com.template.app.common.integration.email;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircuitBreakerEmailService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public boolean sendEmail(String to, String subject, String body) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("email-service");

        Supplier<Boolean> emailSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> sendEmailInternal(to, subject, body)
        );

        try {
            return emailSupplier.get();
        } catch (Exception e) {
            log.error("Failed to send email after circuit breaker, using fallback", e);
            return sendEmailFallback(to, subject, body);
        }
    }

    private boolean sendEmailInternal(String to, String subject, String body) {
        // TODO: Implement actual email sending logic (e.g., SendGrid, AWS SES)
        log.info("Sending email to {}: {}", to, subject);

        // Simulate email sending
        // In production, replace with actual email provider integration
        try {
            // Simulate network call
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }
    }

    private boolean sendEmailFallback(String to, String subject, String body) {
        // Fallback: Log the message for manual processing or queue for retry
        log.warn("Email fallback triggered for {}: {}", to, subject);
        // TODO: Store in database for manual review or retry queue
        return false;
    }

    public String getCircuitBreakerState() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("email-service");
        return circuitBreaker.getState().name();
    }
}
