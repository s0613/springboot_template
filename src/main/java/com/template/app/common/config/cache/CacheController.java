package com.template.app.common.config.cache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
@Tag(name = "Cache Management", description = "Cache statistics and management endpoints")
public class CacheController {

    private final CacheMetricsService cacheMetricsService;

    @GetMapping("/metrics")
    @Operation(summary = "Cache metrics", description = "Returns Redis cache statistics and metrics")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("metrics", cacheMetricsService.getCacheMetrics());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Cache health", description = "Returns Redis cache health status")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("health", cacheMetricsService.getCacheHealth());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cache by pattern", description = "Clears cache entries matching the specified pattern")
    public ResponseEntity<Map<String, Object>> clearCache(@RequestParam String pattern) {
        Map<String, Object> response = new HashMap<>();
        boolean success = cacheMetricsService.clearCache(pattern);
        response.put("success", success);
        response.put("pattern", pattern);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear-all")
    @Operation(summary = "Clear all cache", description = "Clears all cache entries (use with caution)")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        Map<String, Object> response = new HashMap<>();
        boolean success = cacheMetricsService.clearAllCache();
        response.put("success", success);
        response.put("message", success ? "All cache cleared" : "Failed to clear cache");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
