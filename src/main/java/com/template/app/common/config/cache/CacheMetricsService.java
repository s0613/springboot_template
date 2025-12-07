package com.template.app.common.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMetricsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get Redis connection info
            RedisConnection connection = redisConnectionFactory.getConnection();

            try {
                // Get Redis server info
                Properties info = connection.info();
                if (info != null) {
                    metrics.put("redisVersion", info.getProperty("redis_version"));
                    metrics.put("uptimeInSeconds", info.getProperty("uptime_in_seconds"));
                    metrics.put("connectedClients", info.getProperty("connected_clients"));
                    metrics.put("usedMemory", info.getProperty("used_memory_human"));
                    metrics.put("usedMemoryPeak", info.getProperty("used_memory_peak_human"));
                }

                // Get database size
                Long dbSize = connection.dbSize();
                metrics.put("totalKeys", dbSize);

                // Count keys by pattern
                metrics.put("keysByPattern", getKeyCountsByPattern());

            } finally {
                connection.close();
            }

            metrics.put("healthy", true);

        } catch (Exception e) {
            log.error("Failed to retrieve Redis metrics", e);
            metrics.put("healthy", false);
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    private Map<String, Long> getKeyCountsByPattern() {
        Map<String, Long> patternCounts = new HashMap<>();

        try {
            // Count keys for common patterns
            String[] patterns = {
                    "rate_limit:*",
                    "session:*",
                    "cache:*",
                    "verification:*"
            };

            for (String pattern : patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                patternCounts.put(pattern, keys != null ? (long) keys.size() : 0L);
            }

        } catch (Exception e) {
            log.error("Failed to count keys by pattern", e);
        }

        return patternCounts;
    }

    public boolean clearCache(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} keys matching pattern: {}", keys.size(), pattern);
                return true;
            }
            log.info("No keys found matching pattern: {}", pattern);
            return true;
        } catch (Exception e) {
            log.error("Failed to clear cache for pattern: {}", pattern, e);
            return false;
        }
    }

    public boolean clearAllCache() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                connection.flushDb();
                log.info("Cleared all cache");
                return true;
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
            return false;
        }
    }

    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test Redis connectivity with a simple ping
            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                String pingResponse = connection.ping();
                health.put("status", "PONG".equals(pingResponse) ? "UP" : "DOWN");
                health.put("pingResponse", pingResponse);
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }

        return health;
    }
}
