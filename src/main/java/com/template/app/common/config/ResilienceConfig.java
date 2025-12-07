package com.template.app.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Register event listeners for monitoring
        registry.circuitBreaker("sms-service").getEventPublisher()
                .onStateTransition(event -> log.info("SMS Service Circuit Breaker State Transition: {}", event))
                .onError(event -> log.error("SMS Service Circuit Breaker Error: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("SMS Service Circuit Breaker Success"));

        registry.circuitBreaker("email-service").getEventPublisher()
                .onStateTransition(event -> log.info("Email Service Circuit Breaker State Transition: {}", event))
                .onError(event -> log.error("Email Service Circuit Breaker Error: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("Email Service Circuit Breaker Success"));

        registry.circuitBreaker("sns-service").getEventPublisher()
                .onStateTransition(event -> log.info("SNS Service Circuit Breaker State Transition: {}", event))
                .onError(event -> log.error("SNS Service Circuit Breaker Error: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("SNS Service Circuit Breaker Success"));

        return registry;
    }

    @Bean
    public CircuitBreaker smsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("sms-service");
    }

    @Bean
    public CircuitBreaker emailCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("email-service");
    }

    @Bean
    public CircuitBreaker snsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("sns-service");
    }
}
