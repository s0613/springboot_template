package com.template.app.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 30;
    private static final String KEY_PREFIX = "login_attempts:";

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isBlocked(String phoneNumber) {
        Object attempts = redisTemplate.opsForValue().get(KEY_PREFIX + phoneNumber);
        if (attempts == null) {
            return false;
        }
        // Handle both Integer and Long from Redis
        long attemptCount = ((Number) attempts).longValue();
        boolean blocked = attemptCount >= MAX_ATTEMPTS;
        if (blocked) {
            log.warn("Login blocked for phone: {}", maskPhoneNumber(phoneNumber));
        }
        return blocked;
    }

    public void recordFailedAttempt(String phoneNumber) {
        String key = KEY_PREFIX + phoneNumber;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
        log.info("Failed login attempt {} for phone: {}", attempts, maskPhoneNumber(phoneNumber));
    }

    public void resetAttempts(String phoneNumber) {
        redisTemplate.delete(KEY_PREFIX + phoneNumber);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
