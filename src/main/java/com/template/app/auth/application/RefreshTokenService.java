package com.template.app.auth.application;

import com.template.app.auth.api.dto.TokenResponse;
import com.template.app.auth.domain.entity.User;
import com.template.app.auth.infrastructure.exception.InvalidTokenException;
import com.template.app.auth.infrastructure.exception.UserNotFoundException;
import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    public String createRefreshToken(String userId) {
        return jwtTokenProvider.generateRefreshToken(userId);
    }

    @Transactional
    public TokenResponse rotateRefreshToken(String oldRefreshToken) {
        // Validate old refresh token
        if (!jwtTokenProvider.validateToken(oldRefreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(oldRefreshToken);
        String redisKey = "refresh_token:" + userId;

        // ATOMIC operation: get and delete in single operation to prevent race condition
        String storedToken = (String) redisTemplate.opsForValue().getAndDelete(redisKey);

        if (storedToken == null) {
            log.warn("Refresh token reuse or missing token detected for user: {}", userId);
            throw new InvalidTokenException("Token already used or invalid");
        }

        if (!oldRefreshToken.equals(storedToken)) {
            log.warn("Refresh token mismatch detected for user: {}", userId);
            throw new InvalidTokenException("Token mismatch detected");
        }

        // Blacklist old refresh token
        jwtTokenProvider.blacklistToken(oldRefreshToken);

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                userId,
                getUserRoles(userId)
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .build();
    }

    public boolean isBlacklisted(String token) {
        return jwtTokenProvider.isBlacklisted(token);
    }

    private String[] getUserRoles(String userId) {
        try {
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElse(null);

            if (user != null) {
                // Return user type and account type as roles
                return new String[]{
                        user.getUserType().name(),
                        user.getAccountType().name()
                };
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format: {}", userId);
        }

        // Fallback to default roles if user not found (for tests or edge cases)
        log.debug("Using default roles for user: {}", userId);
        return new String[]{"USER"};
    }
}
