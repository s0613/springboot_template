package com.template.app.auth.application;

import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.infrastructure.security.LoginAttemptService;
import com.template.app.auth.api.dto.TokenResponse;
import com.template.app.auth.api.dto.request.LoginRequest;
import com.template.app.auth.api.dto.request.SignupRequest;
import com.template.app.auth.api.dto.response.UserResponse;
import com.template.app.auth.domain.entity.User;
import com.template.app.auth.infrastructure.exception.AccountLockedException;
import com.template.app.auth.infrastructure.exception.InvalidCredentialsException;
import com.template.app.auth.infrastructure.exception.UserAlreadyExistsException;
import com.template.app.auth.infrastructure.exception.UserNotFoundException;
import com.template.app.auth.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SmsVerificationService smsVerificationService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        // Verify phone number (SMS verification should be completed before signup)
        if (!smsVerificationService.isVerified(request.getPhoneNumber())) {
            throw new IllegalStateException("전화번호 인증이 필요합니다");
        }

        // Check if user already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .email(request.getEmail())
                .consentPrivacy(request.getConsentPrivacy())
                .consentService(request.getConsentService())
                .consentMarketing(request.getConsentMarketing() != null ? request.getConsentMarketing() : false)
                .accountType(request.getAccountType())
                .userType(User.UserType.MASTER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getPhoneNumber());

        // Clear verification status after successful signup
        smsVerificationService.clearVerification(request.getPhoneNumber());

        // Generate tokens
        return generateTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String phoneNumber = request.getPhoneNumber();

        // Check if account is locked
        if (loginAttemptService.isBlocked(phoneNumber)) {
            throw new AccountLockedException("계정이 일시적으로 잠겼습니다. 30분 후에 다시 시도해주세요.");
        }

        // Find user by phone number
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailedAttempt(phoneNumber);
                    return new UserNotFoundException("User not found");
                });

        // Check if user is active
        if (!user.getIsActive()) {
            loginAttemptService.recordFailedAttempt(phoneNumber);
            throw new InvalidCredentialsException("Account is deactivated");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailedAttempt(phoneNumber);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Success - reset attempts
        loginAttemptService.resetAttempts(phoneNumber);

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        log.info("User logged in: {}", maskPhoneNumber(phoneNumber));

        // Generate tokens
        return generateTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return UserResponse.from(user);
    }

    @Transactional
    public void logout(String accessToken) {
        // Blacklist the access token
        jwtTokenProvider.blacklistToken(accessToken);
        log.info("User logged out");
    }

    private TokenResponse generateTokens(User user) {
        String[] roles = {user.getUserType().name(), user.getAccountType().name()};

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                roles
        );

        String refreshToken = refreshTokenService.createRefreshToken(
                user.getId().toString()
        );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .build();
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
