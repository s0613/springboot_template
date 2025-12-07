package com.template.app.scheduler.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_job_history", indexes = {
        @Index(name = "idx_job_history_name", columnList = "job_name"),
        @Index(name = "idx_job_history_status", columnList = "status"),
        @Index(name = "idx_job_history_started_at", columnList = "started_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 200)
    private String jobName;

    @Column(name = "job_group", length = 100)
    private String jobGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "instance_id", length = 100)
    private String instanceId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "items_processed")
    private Integer itemsProcessed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum JobStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        SKIPPED
    }

    public void markSuccess(String message, Integer itemsProcessed) {
        this.status = JobStatus.SUCCESS;
        this.finishedAt = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis();
        this.resultMessage = message;
        this.itemsProcessed = itemsProcessed;
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startedAt, finishedAt).toMillis();
        this.errorMessage = errorMessage;
    }

    public void markSkipped(String reason) {
        this.status = JobStatus.SKIPPED;
        this.finishedAt = LocalDateTime.now();
        this.resultMessage = reason;
    }
}
