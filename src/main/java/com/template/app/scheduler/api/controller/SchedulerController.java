package com.template.app.scheduler.api.controller;

import com.template.app.common.dto.ApiResponse;
import com.template.app.scheduler.domain.entity.SchedulerJobHistory;
import com.template.app.scheduler.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
@Tag(name = "Scheduler", description = "Scheduler monitoring APIs (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class SchedulerController {

    private final SchedulerService schedulerService;

    @GetMapping("/jobs/{jobName}/history")
    @Operation(summary = "Get job execution history")
    public ResponseEntity<ApiResponse<Page<SchedulerJobHistory>>> getJobHistory(
            @PathVariable String jobName,
            Pageable pageable
    ) {
        Page<SchedulerJobHistory> history = schedulerService.getJobHistory(jobName, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/jobs/recent")
    @Operation(summary = "Get recent job executions")
    public ResponseEntity<ApiResponse<List<SchedulerJobHistory>>> getRecentJobs(
            @RequestParam(defaultValue = "24") int hours
    ) {
        List<SchedulerJobHistory> jobs = schedulerService.getRecentJobs(hours);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/jobs/{jobName}/status")
    @Operation(summary = "Check if a job is running")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatus(@PathVariable String jobName) {
        boolean isRunning = schedulerService.isJobRunning(jobName);
        var latestExecution = schedulerService.getLatestJobExecution(jobName);

        Map<String, Object> status = Map.of(
                "jobName", jobName,
                "isRunning", isRunning,
                "latestExecution", latestExecution.orElse(null)
        );

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get job statistics")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Long>>>> getJobStats(
            @RequestParam(defaultValue = "7") int days
    ) {
        Map<String, Map<String, Long>> stats = schedulerService.getJobStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/jobs/{jobName}/avg-duration")
    @Operation(summary = "Get average job duration")
    public ResponseEntity<ApiResponse<Double>> getAverageJobDuration(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "7") int days
    ) {
        Double avgDuration = schedulerService.getAverageJobDuration(jobName, days);
        return ResponseEntity.ok(ApiResponse.success(avgDuration));
    }

    @GetMapping("/jobs/failed")
    @Operation(summary = "Get failed jobs")
    public ResponseEntity<ApiResponse<Page<SchedulerJobHistory>>> getFailedJobs(Pageable pageable) {
        Page<SchedulerJobHistory> failedJobs = schedulerService.getFailedJobs(pageable);
        return ResponseEntity.ok(ApiResponse.success(failedJobs));
    }

    @GetMapping("/health")
    @Operation(summary = "Get scheduler health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedulerHealth() {
        Map<String, Object> health = schedulerService.getSchedulerHealth();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @DeleteMapping("/history/cleanup")
    @Operation(summary = "Clean up old job history")
    public ResponseEntity<ApiResponse<Void>> cleanupHistory(
            @RequestParam(defaultValue = "30") int retentionDays
    ) {
        schedulerService.cleanupOldHistory(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
