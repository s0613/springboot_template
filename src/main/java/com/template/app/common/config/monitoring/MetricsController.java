package com.template.app.common.config.monitoring;

import com.template.app.common.config.database.DatabaseMetricsService;
import com.template.app.common.integration.email.CircuitBreakerEmailService;
import com.template.app.common.integration.sms.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Application metrics and monitoring endpoints")
public class MetricsController {

    private final DatabaseMetricsService databaseMetricsService;
    private final SmsService smsService;
    private final CircuitBreakerEmailService emailService;

    @GetMapping("/database")
    @Operation(summary = "Database connection pool metrics", description = "Returns HikariCP connection pool statistics")
    public ResponseEntity<Map<String, Object>> getDatabaseMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("metrics", databaseMetricsService.getDetailedMetrics());
        response.put("healthy", databaseMetricsService.isHealthy());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/circuit-breakers")
    @Operation(summary = "Circuit breaker status", description = "Returns the state of all circuit breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("smsService", smsService.getCircuitBreakerState());
        response.put("emailService", emailService.getCircuitBreakerState());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
