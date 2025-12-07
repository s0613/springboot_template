package com.template.app.auth.application;

import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.api.dto.request.AccountDeletionRequest;
import com.template.app.auth.api.dto.response.AccountDeletionResponse;
import com.template.app.auth.domain.entity.User;
import com.template.app.auth.infrastructure.exception.InvalidPasswordException;
import com.template.app.auth.infrastructure.exception.UserNotFoundException;
import com.template.app.auth.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int RECOVERY_PERIOD_DAYS = 14;

    /**
     * Delete (deactivate) a user account
     *
     * @param userId User ID to delete
     * @param request Deletion request with password and reason
     * @param accessToken Current access token (to be invalidated)
     * @return AccountDeletionResponse with deletion details
     */
    @Transactional
    public AccountDeletionResponse deleteAccount(Long userId, AccountDeletionRequest request, String accessToken) {
        log.info("Processing account deletion for user: {}", userId);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Verify user is active
        if (!user.getIsActive()) {
            throw new IllegalStateException("계정이 이미 삭제되었습니다");
        }

        // Verify password for non-OAuth users
        if (user.isPasswordUser()) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new InvalidPasswordException("비밀번호를 입력해주세요");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Invalid password attempt for user deletion: {}", userId);
                throw new InvalidPasswordException("비밀번호가 일치하지 않습니다");
            }
        }

        // Mark user as deleted (soft delete)
        user.markAsDeleted(request.getReason(), request.getDetailReason());
        userRepository.save(user);

        log.info("User account marked as deleted: userId={}, reason={}", userId, request.getReason());

        // Disconnect family relationships
        disconnectFamilyRelationships(user);

        // Invalidate all tokens
        invalidateUserTokens(userId, accessToken);

        // Calculate recovery expiry date
        LocalDateTime recoveryExpiresAt = user.getDeletedAt().plusDays(RECOVERY_PERIOD_DAYS);

        // Store deletion schedule in Redis for tracking
        String deletionScheduleKey = "deletion_schedule:" + user.getId();
        redisTemplate.opsForValue().set(
                deletionScheduleKey,
                recoveryExpiresAt.toString(),
                RECOVERY_PERIOD_DAYS + 1, // Extra day buffer
                TimeUnit.DAYS
        );

        // Build response
        return AccountDeletionResponse.builder()
                .userId(userId.toString())
                .deletedAt(user.getDeletedAt())
                .message("회원 탈퇴가 완료되었습니다. " + RECOVERY_PERIOD_DAYS + "일 이내 복구 가능합니다.")
                .recoveryPeriodDays(RECOVERY_PERIOD_DAYS)
                .recoveryExpiresAt(recoveryExpiresAt)
                .build();
    }

    /**
     * Recover a deleted account within the recovery period
     *
     * @param userId User ID to recover
     * @return Success message
     */
    @Transactional
    public String recoverAccount(Long userId) {
        log.info("Processing account recovery for user: {}", userId);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Verify user is deleted
        if (!user.isDeleted()) {
            throw new IllegalStateException("계정이 삭제되지 않았습니다");
        }

        // Verify recovery period has not expired
        LocalDateTime recoveryDeadline = user.getDeletedAt().plusDays(RECOVERY_PERIOD_DAYS);
        if (LocalDateTime.now().isAfter(recoveryDeadline)) {
            log.warn("Recovery period expired for user: {}", userId);
            throw new IllegalArgumentException("복구 기간이 만료되었습니다. 새로운 계정을 생성해주세요.");
        }

        // Restore account
        user.restoreAccount();
        userRepository.save(user);

        log.info("User account recovered successfully: {}", userId);

        return "계정이 성공적으로 복구되었습니다";
    }

    /**
     * Invalidate all tokens for a user
     */
    private void invalidateUserTokens(Long userId, String accessToken) {
        log.debug("Invalidating tokens for user: {}", userId);

        // Blacklist the current access token
        if (accessToken != null && !accessToken.isBlank()) {
            jwtTokenProvider.blacklistToken(accessToken);
        }

        // Remove refresh token from Redis
        String refreshTokenKey = "refresh_token:" + userId;
        redisTemplate.delete(refreshTokenKey);

        log.debug("Tokens invalidated for user: {}", userId);
    }

    /**
     * Schedule permanent data deletion after recovery period
     * Runs daily at 2 AM to process expired account deletions
     */
    @Scheduled(cron = "0 0 2 * * *") // Run every day at 2:00 AM
    @Transactional
    public void scheduleDataDeletion() {
        log.info("Starting scheduled account deletion job at {}", LocalDateTime.now());

        // Calculate the expiry date (deletedAt + RECOVERY_PERIOD_DAYS)
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(RECOVERY_PERIOD_DAYS);

        // Find all users whose recovery period has expired
        List<User> expiredUsers = userRepository.findExpiredDeletions(expiryDate);

        log.info("Found {} users with expired recovery period", expiredUsers.size());

        int successCount = 0;
        int failureCount = 0;

        for (User user : expiredUsers) {
            try {
                // Disconnect family relationships before permanent deletion
                disconnectFamilyRelationships(user);

                // Permanently delete the user
                permanentlyDeleteUser(user.getId());

                // Clean up Redis deletion schedule key if exists
                String deletionScheduleKey = "deletion_schedule:" + user.getId();
                redisTemplate.delete(deletionScheduleKey);

                successCount++;
                log.info("Successfully processed permanent deletion for user: {}", user.getId());
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to permanently delete user {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("Scheduled deletion job completed. Success: {}, Failures: {}", successCount, failureCount);
    }

    /**
     * Disconnect family relationships when deleting an account
     * Handles both master and sub-account scenarios
     *
     * @param user User being deleted
     */
    @Transactional
    public void disconnectFamilyRelationships(User user) {
        log.info("Disconnecting family relationships for user: {}", user.getId());

        if (user.getUserType() == User.UserType.MASTER) {
            // If deleted user is a master, handle all sub-accounts
            List<User> subAccounts = userRepository.findByMasterUser(user);

            if (!subAccounts.isEmpty()) {
                log.info("User {} is a master with {} sub-accounts", user.getId(), subAccounts.size());

                for (User subAccount : subAccounts) {
                    // Option 1: Delete sub-accounts as well (cascade deletion)
                    // This is the safer approach for GDPR compliance
                    log.info("Marking sub-account {} for deletion due to master account deletion", subAccount.getId());
                    subAccount.markAsDeleted("MASTER_ACCOUNT_DELETED", "Master account was deleted");
                    userRepository.save(subAccount);

                    // Option 2: Orphan the sub-accounts (make them independent)
                    // Uncomment if you prefer this approach:
                    // subAccount.setMasterUser(null);
                    // subAccount.setUserType(User.UserType.MASTER);
                    // userRepository.save(subAccount);
                    // log.info("Orphaned sub-account {} to independent account", subAccount.getId());
                }

                log.info("Processed {} sub-accounts for master user {}", subAccounts.size(), user.getId());
            }
        } else if (user.getUserType() == User.UserType.SUB_ACCOUNT && user.getMasterUser() != null) {
            // If deleted user is a sub-account, just remove the relationship
            User masterUser = user.getMasterUser();
            log.info("User {} is a sub-account of master {}, disconnecting relationship", user.getId(), masterUser.getId());

            user.setMasterUser(null);
            user.setLoginCode(null);
            userRepository.save(user);

            log.info("Disconnected sub-account {} from master {}", user.getId(), masterUser.getId());
        }

        log.info("Family relationships disconnected for user: {}", user.getId());
    }

    /**
     * Permanently delete user data (called by scheduled job)
     * This would be invoked by a background job after the recovery period
     */
    @Transactional
    public void permanentlyDeleteUser(Long userId) {
        log.info("Permanently deleting user data: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Verify user is deleted and recovery period has passed
        if (!user.isDeleted()) {
            throw new IllegalStateException("Cannot permanently delete an active account");
        }

        LocalDateTime recoveryDeadline = user.getDeletedAt().plusDays(RECOVERY_PERIOD_DAYS);
        if (LocalDateTime.now().isBefore(recoveryDeadline)) {
            throw new IllegalStateException("Recovery period has not expired yet");
        }

        // Anonymize or delete user data
        // For GDPR/data protection compliance, you may want to:
        // 1. Anonymize instead of delete (for audit trail)
        // 2. Delete associated data (assessments, responses, etc.)
        // 3. Keep minimal audit record

        user.setPhoneNumber("DELETED_" + userId);
        user.setName("삭제된 사용자");
        user.setEmail(null);
        user.setOauthId(null);
        user.setPassword(null);

        userRepository.save(user);

        // Or completely delete:
        // userRepository.delete(user);

        log.info("User data permanently deleted/anonymized: {}", userId);
    }
}
