package com.template.app.common.config.database;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMetricsService {

    private final DataSource dataSource;

    public Map<String, Object> getPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

            if (poolMXBean != null) {
                metrics.put("activeConnections", poolMXBean.getActiveConnections());
                metrics.put("idleConnections", poolMXBean.getIdleConnections());
                metrics.put("totalConnections", poolMXBean.getTotalConnections());
                metrics.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());

                // Calculate pool utilization percentage
                int total = poolMXBean.getTotalConnections();
                int active = poolMXBean.getActiveConnections();
                double utilization = total > 0 ? (active * 100.0 / total) : 0;
                metrics.put("poolUtilization", String.format("%.2f%%", utilization));

                // Health status
                metrics.put("healthy", poolMXBean.getThreadsAwaitingConnection() == 0);
            } else {
                log.warn("HikariPoolMXBean is null, unable to retrieve metrics");
                metrics.put("error", "Metrics not available");
            }
        } else {
            log.warn("DataSource is not HikariDataSource, unable to retrieve metrics");
            metrics.put("error", "Not a HikariDataSource");
        }

        return metrics;
    }

    public Map<String, Object> getDetailedMetrics() {
        Map<String, Object> metrics = getPoolMetrics();

        if (dataSource instanceof HikariDataSource hikariDataSource) {
            metrics.put("poolName", hikariDataSource.getPoolName());
            metrics.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
            metrics.put("minimumIdle", hikariDataSource.getMinimumIdle());
            metrics.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
            metrics.put("idleTimeout", hikariDataSource.getIdleTimeout());
            metrics.put("maxLifetime", hikariDataSource.getMaxLifetime());
        }

        return metrics;
    }

    public boolean isHealthy() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            if (poolMXBean != null) {
                // Pool is healthy if there are no threads waiting and we have idle connections
                return poolMXBean.getThreadsAwaitingConnection() == 0
                        && poolMXBean.getIdleConnections() > 0;
            }
        }
        return false;
    }
}
