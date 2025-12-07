package com.template.app.notification.repository;

import com.template.app.notification.entity.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, Long> {

    @Query("SELECT f FROM FailedNotification f WHERE " +
           "f.status IN ('PENDING', 'RETRYING') AND " +
           "f.retryCount < f.maxRetries AND " +
           "f.nextRetryAt <= :now")
    List<FailedNotification> findPendingRetries(LocalDateTime now);

    @Query("SELECT f FROM FailedNotification f WHERE " +
           "f.status = 'FAILED' AND " +
           "f.retryCount >= f.maxRetries AND " +
           "f.createdAt >= :since")
    List<FailedNotification> findRecentFailures(LocalDateTime since);

    long countByStatusAndCreatedAtAfter(String status, LocalDateTime since);
}
