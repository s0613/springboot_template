package com.template.app.common.config.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Health check and readiness endpoints")
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final ApplicationAvailability applicationAvailability;
    private final DataSource dataSource;

    @GetMapping("/health")
    @Operation(summary = "Overall health check", description = "Returns the overall health status of the application")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> healthInfo = new HashMap<>();

            // Get overall health from Spring Boot Actuator
            HealthComponent healthComponent = healthEndpoint.health();
            healthInfo.put("status", healthComponent.getStatus().getCode());

            // Add custom health checks
            healthInfo.put("database", checkDatabase());
            healthInfo.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            log.error("Health check failed", e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("status", "DOWN");
            errorInfo.put("error", e.getMessage());
            errorInfo.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(errorInfo);
        }
    }

    @GetMapping("/health/live")
    @Operation(summary = "Liveness probe", description = "Kubernetes liveness probe endpoint")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        LivenessState livenessState = applicationAvailability.getLivenessState();

        response.put("status", livenessState.toString());
        response.put("timestamp", System.currentTimeMillis());

        if (livenessState == LivenessState.CORRECT) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/health/ready")
    @Operation(summary = "Readiness probe", description = "Kubernetes readiness probe endpoint")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();
        ReadinessState readinessState = applicationAvailability.getReadinessState();

        response.put("status", readinessState.toString());
        response.put("database", checkDatabase());
        response.put("timestamp", System.currentTimeMillis());

        if (readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("validConnection", isValid);
            return dbHealth;
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            return dbHealth;
        }
    }
}
