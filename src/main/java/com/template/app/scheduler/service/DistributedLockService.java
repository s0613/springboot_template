package com.template.app.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed lock service.
 * Prevents multiple instances from running the same scheduled job simultaneously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String INSTANCE_ID = UUID.randomUUID().toString();

    /**
     * Try to acquire a lock
     *
     * @param lockKey  the lock identifier
     * @param duration how long the lock should be held
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryLock(String lockKey, Duration duration) {
        String key = LOCK_PREFIX + lockKey;
        String value = INSTANCE_ID + ":" + System.currentTimeMillis();

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, duration);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired: {} by instance {}", lockKey, INSTANCE_ID);
            return true;
        }

        log.debug("Lock not acquired: {} (held by another instance)", lockKey);
        return false;
    }

    /**
     * Try to acquire a lock with retry
     *
     * @param lockKey     the lock identifier
     * @param duration    how long the lock should be held
     * @param maxRetries  maximum number of retry attempts
     * @param retryDelay  delay between retries
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryLockWithRetry(String lockKey, Duration duration, int maxRetries, Duration retryDelay) {
        for (int i = 0; i <= maxRetries; i++) {
            if (tryLock(lockKey, duration)) {
                return true;
            }
            if (i < maxRetries) {
                try {
                    TimeUnit.MILLISECONDS.sleep(retryDelay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Release a lock
     *
     * @param lockKey the lock identifier
     * @return true if lock was released, false if lock was not held by this instance
     */
    public boolean unlock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String value = redisTemplate.opsForValue().get(key);

        if (value != null && value.startsWith(INSTANCE_ID + ":")) {
            redisTemplate.delete(key);
            log.debug("Lock released: {}", lockKey);
            return true;
        }

        log.debug("Lock not released: {} (not held by this instance)", lockKey);
        return false;
    }

    /**
     * Check if a lock is held
     *
     * @param lockKey the lock identifier
     * @return true if lock exists
     */
    public boolean isLocked(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Check if this instance holds the lock
     *
     * @param lockKey the lock identifier
     * @return true if this instance holds the lock
     */
    public boolean isLockedByMe(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String value = redisTemplate.opsForValue().get(key);
        return value != null && value.startsWith(INSTANCE_ID + ":");
    }

    /**
     * Extend lock expiration
     *
     * @param lockKey  the lock identifier
     * @param duration new duration from now
     * @return true if lock was extended, false if lock is not held by this instance
     */
    public boolean extendLock(String lockKey, Duration duration) {
        String key = LOCK_PREFIX + lockKey;
        String value = redisTemplate.opsForValue().get(key);

        if (value != null && value.startsWith(INSTANCE_ID + ":")) {
            redisTemplate.expire(key, duration);
            log.debug("Lock extended: {} for {}", lockKey, duration);
            return true;
        }

        return false;
    }

    /**
     * Execute a task with a distributed lock
     *
     * @param lockKey  the lock identifier
     * @param duration how long the lock should be held
     * @param task     the task to execute
     * @return true if task was executed, false if lock was not acquired
     */
    public boolean executeWithLock(String lockKey, Duration duration, Runnable task) {
        if (!tryLock(lockKey, duration)) {
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * Get the current instance ID
     */
    public String getInstanceId() {
        return INSTANCE_ID;
    }
}
