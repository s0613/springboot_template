package com.template.app.scheduler.repository;

import com.template.app.scheduler.domain.entity.SchedulerJobHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulerJobHistoryRepository extends JpaRepository<SchedulerJobHistory, Long> {

    Page<SchedulerJobHistory> findByJobNameOrderByStartedAtDesc(String jobName, Pageable pageable);

    Page<SchedulerJobHistory> findByStatus(SchedulerJobHistory.JobStatus status, Pageable pageable);

    @Query("SELECT h FROM SchedulerJobHistory h WHERE h.jobName = :jobName ORDER BY h.startedAt DESC LIMIT 1")
    Optional<SchedulerJobHistory> findLatestByJobName(@Param("jobName") String jobName);

    @Query("SELECT h FROM SchedulerJobHistory h WHERE h.jobName = :jobName AND h.status = :status ORDER BY h.startedAt DESC LIMIT 1")
    Optional<SchedulerJobHistory> findLatestByJobNameAndStatus(
            @Param("jobName") String jobName,
            @Param("status") SchedulerJobHistory.JobStatus status
    );

    @Query("SELECT h FROM SchedulerJobHistory h WHERE h.startedAt >= :since ORDER BY h.startedAt DESC")
    List<SchedulerJobHistory> findRecentJobs(@Param("since") LocalDateTime since);

    @Query("SELECT h.jobName, h.status, COUNT(h) FROM SchedulerJobHistory h " +
            "WHERE h.startedAt >= :since GROUP BY h.jobName, h.status")
    List<Object[]> getJobStatsSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(h.durationMs) FROM SchedulerJobHistory h " +
            "WHERE h.jobName = :jobName AND h.status = 'SUCCESS' AND h.startedAt >= :since")
    Double getAverageDuration(@Param("jobName") String jobName, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(h) FROM SchedulerJobHistory h WHERE h.jobName = :jobName AND h.status = 'RUNNING'")
    Long countRunningJobs(@Param("jobName") String jobName);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
