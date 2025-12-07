package com.template.app.scheduler.aspect;

import com.template.app.scheduler.annotation.ScheduledWithLock;
import com.template.app.scheduler.domain.entity.SchedulerJobHistory;
import com.template.app.scheduler.repository.SchedulerJobHistoryRepository;
import com.template.app.scheduler.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SchedulerLockAspect {

    private final DistributedLockService lockService;
    private final SchedulerJobHistoryRepository jobHistoryRepository;

    @Around("@annotation(scheduledWithLock)")
    public Object executeWithLock(ProceedingJoinPoint joinPoint, ScheduledWithLock scheduledWithLock) throws Throwable {
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String lockKey = scheduledWithLock.lockKey().isEmpty() ? methodName : scheduledWithLock.lockKey();
        Duration lockDuration = Duration.ofSeconds(scheduledWithLock.lockDurationSeconds());

        // Try to acquire lock
        if (!lockService.tryLock(lockKey, lockDuration)) {
            if (scheduledWithLock.skipIfLocked()) {
                log.debug("Job {} skipped - lock held by another instance", lockKey);
                if (scheduledWithLock.recordHistory()) {
                    recordSkipped(lockKey, scheduledWithLock.jobGroup(), "Lock held by another instance");
                }
                return null;
            } else {
                log.debug("Job {} waiting for lock...", lockKey);
                // Wait and retry could be implemented here if needed
                return null;
            }
        }

        SchedulerJobHistory history = null;
        if (scheduledWithLock.recordHistory()) {
            history = startJobHistory(lockKey, scheduledWithLock.jobGroup());
        }

        try {
            log.info("Starting scheduled job: {}", lockKey);
            Object result = joinPoint.proceed();
            log.info("Completed scheduled job: {}", lockKey);

            if (history != null) {
                completeJobHistory(history, result);
            }

            return result;
        } catch (Throwable e) {
            log.error("Failed scheduled job: {}", lockKey, e);

            if (history != null) {
                failJobHistory(history, e);
            }

            throw e;
        } finally {
            lockService.unlock(lockKey);
        }
    }

    private SchedulerJobHistory startJobHistory(String jobName, String jobGroup) {
        SchedulerJobHistory history = SchedulerJobHistory.builder()
                .jobName(jobName)
                .jobGroup(jobGroup)
                .status(SchedulerJobHistory.JobStatus.RUNNING)
                .instanceId(lockService.getInstanceId())
                .startedAt(LocalDateTime.now())
                .build();
        return jobHistoryRepository.save(history);
    }

    private void completeJobHistory(SchedulerJobHistory history, Object result) {
        String message = "Completed successfully";
        Integer itemsProcessed = null;

        if (result instanceof Integer) {
            itemsProcessed = (Integer) result;
            message = "Processed " + itemsProcessed + " items";
        } else if (result instanceof SchedulerJobResult jobResult) {
            message = jobResult.message();
            itemsProcessed = jobResult.itemsProcessed();
        }

        history.markSuccess(message, itemsProcessed);
        jobHistoryRepository.save(history);
    }

    private void failJobHistory(SchedulerJobHistory history, Throwable e) {
        history.markFailed(e.getMessage());
        jobHistoryRepository.save(history);
    }

    private void recordSkipped(String jobName, String jobGroup, String reason) {
        SchedulerJobHistory history = SchedulerJobHistory.builder()
                .jobName(jobName)
                .jobGroup(jobGroup)
                .status(SchedulerJobHistory.JobStatus.SKIPPED)
                .instanceId(lockService.getInstanceId())
                .startedAt(LocalDateTime.now())
                .build();
        history.markSkipped(reason);
        jobHistoryRepository.save(history);
    }

    /**
     * Record class for job results with details.
     */
    public record SchedulerJobResult(String message, Integer itemsProcessed) {
    }
}
