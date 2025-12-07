package com.template.app.auth.application;

import com.template.app.common.integration.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    private final SmsService smsService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Send verification code to phone number
     *
     * @param phoneNumber Phone number to send verification code
     * @return true if SMS was sent successfully
     */
    public boolean sendVerificationCode(String phoneNumber) {
        // Generate 6-digit verification code
        String code = generateVerificationCode();

        // Store in Redis with expiry
        String key = getVerificationKey(phoneNumber);
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // Initialize attempt counter
        String attemptKey = getAttemptKey(phoneNumber);
        redisTemplate.opsForValue().set(attemptKey, 0, CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // Send SMS
        String message = String.format("[코그모 안녕] 인증번호는 [%s]입니다. %d분 내에 입력해주세요.",
                code, CODE_EXPIRY_MINUTES);

        boolean sent = smsService.sendSms(phoneNumber, message);

        if (sent) {
            log.info("Verification code sent to: {}", phoneNumber);
        } else {
            log.error("Failed to send verification code to: {}", phoneNumber);
            // Clean up Redis if SMS failed
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
        }

        return sent;
    }

    /**
     * Verify the code entered by user
     *
     * @param phoneNumber Phone number
     * @param code Verification code entered by user
     * @return true if code is valid
     */
    public boolean verifyCode(String phoneNumber, String code) {
        String key = getVerificationKey(phoneNumber);
        String attemptKey = getAttemptKey(phoneNumber);

        // Check if code exists
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            log.warn("No verification code found for: {}", phoneNumber);
            return false;
        }

        // Check attempt count
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);
        if (attempts == null) {
            attempts = 0;
        }

        if (attempts >= MAX_ATTEMPTS) {
            log.warn("Max verification attempts exceeded for: {}", phoneNumber);
            // Delete the code
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            return false;
        }

        // Increment attempt counter
        redisTemplate.opsForValue().increment(attemptKey);

        // Verify code
        if (storedCode.equals(code)) {
            log.info("Verification successful for: {}", phoneNumber);
            // Mark as verified
            String verifiedKey = getVerifiedKey(phoneNumber);
            redisTemplate.opsForValue().set(verifiedKey, true, 10, TimeUnit.MINUTES);

            // Clean up
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);

            return true;
        } else {
            log.warn("Invalid verification code for: {} (attempt {}/{})",
                    phoneNumber, attempts + 1, MAX_ATTEMPTS);
            return false;
        }
    }

    /**
     * Check if phone number is verified
     *
     * @param phoneNumber Phone number to check
     * @return true if verified
     */
    public boolean isVerified(String phoneNumber) {
        String verifiedKey = getVerifiedKey(phoneNumber);
        Boolean verified = (Boolean) redisTemplate.opsForValue().get(verifiedKey);
        return verified != null && verified;
    }

    /**
     * Mark phone number as verified (for testing)
     *
     * @param phoneNumber Phone number
     */
    public void markAsVerified(String phoneNumber) {
        String verifiedKey = getVerifiedKey(phoneNumber);
        redisTemplate.opsForValue().set(verifiedKey, true, 10, TimeUnit.MINUTES);
    }

    /**
     * Clear verification status (after signup completion)
     *
     * @param phoneNumber Phone number
     */
    public void clearVerification(String phoneNumber) {
        redisTemplate.delete(getVerifiedKey(phoneNumber));
        redisTemplate.delete(getVerificationKey(phoneNumber));
        redisTemplate.delete(getAttemptKey(phoneNumber));
    }

    /**
     * Generate random 6-digit verification code
     */
    private String generateVerificationCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000 to 999999
        return String.valueOf(code);
    }

    private String getVerificationKey(String phoneNumber) {
        return "sms:verification:" + phoneNumber;
    }

    private String getAttemptKey(String phoneNumber) {
        return "sms:attempts:" + phoneNumber;
    }

    private String getVerifiedKey(String phoneNumber) {
        return "sms:verified:" + phoneNumber;
    }

    /**
     * Get remaining time for verification code
     *
     * @param phoneNumber Phone number
     * @return Remaining seconds, or -1 if expired/not found
     */
    public long getRemainingTime(String phoneNumber) {
        String key = getVerificationKey(phoneNumber);
        Long expiry = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expiry != null ? expiry : -1;
    }
}
