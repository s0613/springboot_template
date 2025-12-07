package com.template.app.scheduler.service;

import com.template.app.scheduler.domain.entity.SchedulerJobHistory;
import com.template.app.scheduler.repository.SchedulerJobHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final SchedulerJobHistoryRepository jobHistoryRepository;
    private final DistributedLockService lockService;

    /**
     * Get job execution history
     */
    @Transactional(readOnly = true)
    public Page<SchedulerJobHistory> getJobHistory(String jobName, Pageable pageable) {
        return jobHistoryRepository.findByJobNameOrderByStartedAtDesc(jobName, pageable);
    }

    /**
     * Get recent job executions
     */
    @Transactional(readOnly = true)
    public List<SchedulerJobHistory> getRecentJobs(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return jobHistoryRepository.findRecentJobs(since);
    }

    /**
     * Get latest execution for a job
     */
    @Transactional(readOnly = true)
    public Optional<SchedulerJobHistory> getLatestJobExecution(String jobName) {
        return jobHistoryRepository.findLatestByJobName(jobName);
    }

    /**
     * Get job statistics for dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> getJobStats(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = jobHistoryRepository.getJobStatsSince(since);

        Map<String, Map<String, Long>> stats = new HashMap<>();
        for (Object[] row : results) {
            String jobName = (String) row[0];
            SchedulerJobHistory.JobStatus status = (SchedulerJobHistory.JobStatus) row[1];
            Long count = (Long) row[2];

            stats.computeIfAbsent(jobName, k -> new HashMap<>())
                    .put(status.name(), count);
        }

        return stats;
    }

    /**
     * Get average job duration
     */
    @Transactional(readOnly = true)
    public Double getAverageJobDuration(String jobName, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return jobHistoryRepository.getAverageDuration(jobName, since);
    }

    /**
     * Check if a job is currently running
     */
    @Transactional(readOnly = true)
    public boolean isJobRunning(String jobName) {
        return jobHistoryRepository.countRunningJobs(jobName) > 0 || lockService.isLocked(jobName);
    }

    /**
     * Get failed jobs in the last N hours
     */
    @Transactional(readOnly = true)
    public Page<SchedulerJobHistory> getFailedJobs(Pageable pageable) {
        return jobHistoryRepository.findByStatus(SchedulerJobHistory.JobStatus.FAILED, pageable);
    }

    /**
     * Clean up old job history
     */
    @Transactional
    public void cleanupOldHistory(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        jobHistoryRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up job history older than {} days", retentionDays);
    }

    /**
     * Get health status of scheduled jobs
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSchedulerHealth() {
        Map<String, Object> health = new HashMap<>();

        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
        List<SchedulerJobHistory> recentJobs = jobHistoryRepository.findRecentJobs(lastHour);

        long successCount = recentJobs.stream()
                .filter(j -> j.getStatus() == SchedulerJobHistory.JobStatus.SUCCESS)
                .count();
        long failedCount = recentJobs.stream()
                .filter(j -> j.getStatus() == SchedulerJobHistory.JobStatus.FAILED)
                .count();
        long runningCount = recentJobs.stream()
                .filter(j -> j.getStatus() == SchedulerJobHistory.JobStatus.RUNNING)
                .count();

        health.put("lastHourSuccessCount", successCount);
        health.put("lastHourFailedCount", failedCount);
        health.put("currentlyRunning", runningCount);
        health.put("instanceId", lockService.getInstanceId());
        health.put("status", failedCount == 0 ? "HEALTHY" : "DEGRADED");

        return health;
    }
}
