package com.template.app.common.integration.sms;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final NcpSensClient ncpSensClient;

    public boolean sendSms(String phoneNumber, String message) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("sms-service");

        Supplier<Boolean> smsSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> sendSmsInternal(phoneNumber, message)
        );

        try {
            return smsSupplier.get();
        } catch (Exception e) {
            log.error("Failed to send SMS after circuit breaker, using fallback", e);
            return sendSmsFallback(phoneNumber, message);
        }
    }

    private boolean sendSmsInternal(String phoneNumber, String message) {
        log.info("Sending SMS to {}: {}", phoneNumber, message);

        try {
            return ncpSensClient.sendSms(phoneNumber, message);
        } catch (Exception e) {
            log.error("Failed to send SMS via NCP SENS", e);
            throw new RuntimeException("SMS sending failed", e);
        }
    }

    private boolean sendSmsFallback(String phoneNumber, String message) {
        // Fallback: Log the message for manual processing or queue for retry
        log.warn("SMS fallback triggered for {}: {}", phoneNumber, message);
        // TODO: Store in database for manual review or retry queue
        return false;
    }

    public String getCircuitBreakerState() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("sms-service");
        return circuitBreaker.getState().name();
    }
}
