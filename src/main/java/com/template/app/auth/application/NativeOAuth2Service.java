package com.template.app.auth.application;

import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.api.dto.TokenResponse;
import com.template.app.auth.api.dto.response.OAuth2UserInfo;
import com.template.app.auth.domain.entity.User;
import com.template.app.auth.infrastructure.oauth2.apple.AppleTokenVerifierService;
import com.template.app.auth.infrastructure.oauth2.google.GoogleTokenVerifierService;
import com.template.app.auth.infrastructure.oauth2.kakao.KakaoTokenVerifierService;
import com.template.app.auth.infrastructure.repository.UserRepository;
import com.template.app.auth.infrastructure.exception.InvalidOAuth2TokenException;
import com.template.app.auth.infrastructure.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NativeOAuth2Service {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final KakaoTokenVerifierService kakaoTokenVerifierService;
    private final AppleTokenVerifierService appleTokenVerifierService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    private static final long PHONE_NUMBER_HASH_SPACE = 100_000_000_000L;

    /**
     * Authenticate user with Google ID token from native SDK
     */
    @Transactional
    public TokenResponse authenticateWithGoogle(String idToken) {
        // Token verification happens OUTSIDE transaction
        OAuth2UserInfo userInfo = googleTokenVerifierService.verifyIdToken(idToken);
        return authenticateWithUserInfo(userInfo);
    }

    /**
     * Authenticate user with Kakao access token from native SDK
     */
    @Transactional
    public TokenResponse authenticateWithKakao(String accessToken) {
        // Token verification happens OUTSIDE transaction
        OAuth2UserInfo userInfo = kakaoTokenVerifierService.verifyAccessToken(accessToken);
        return authenticateWithUserInfo(userInfo);
    }

    /**
     * Authenticate user with Apple ID token from native SDK
     *
     * @param idToken Apple ID token from native SDK
     * @param name User's name (only provided on first login by Apple)
     * @param email User's email if provided separately by Apple
     */
    @Transactional
    public TokenResponse authenticateWithApple(String idToken, String name, String email) {
        // Token verification happens OUTSIDE transaction
        OAuth2UserInfo userInfo = appleTokenVerifierService.verifyIdToken(idToken, name, email);
        return authenticateWithUserInfo(userInfo);
    }

    /**
     * Authenticate user with verified OAuth2 user info
     * Only database operations happen INSIDE transaction
     * Handles race condition when concurrent requests create the same user
     */
    private TokenResponse authenticateWithUserInfo(OAuth2UserInfo userInfo) {
        try {
            // Only database operations happen INSIDE transaction
            User user = findOrCreateOAuth2User(userInfo);
            return generateTokens(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread already created this user
            log.warn("Race condition detected during OAuth2 user creation for provider={}, providerId={}",
                    userInfo.getProvider(), userInfo.getProviderId());

            User existingUser = userRepository.findByOauthProviderAndOauthId(
                    userInfo.getProvider(),
                    userInfo.getProviderId()
            ).orElseThrow(() -> new InvalidOAuth2TokenException("Failed to authenticate: user not found after race condition"));

            existingUser.updateLastLogin();
            return generateTokens(userRepository.save(existingUser));
        }
    }

    /**
     * Find existing OAuth2 user or create new one
     */
    private User findOrCreateOAuth2User(OAuth2UserInfo userInfo) {
        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
                userInfo.getProvider(),
                userInfo.getProviderId()
        );

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateLastLogin();
            log.info("Existing OAuth2 user logged in: {} ({})",
                     user.getEmail(), userInfo.getProvider());
            return userRepository.save(user);
        }

        // Check if email already exists with different provider
        if (userInfo.getEmail() != null && !userInfo.getEmail().contains("@noreply.cogmo.life")) {
            Optional<User> userByEmail = userRepository.findByEmail(userInfo.getEmail());
            if (userByEmail.isPresent()) {
                User existingUserByEmail = userByEmail.get();
                String existingProvider = existingUserByEmail.getOauthProvider();
                if (existingProvider == null) {
                    existingProvider = "password";
                }
                log.warn("Email {} already exists with provider {}",
                        userInfo.getEmail(), existingProvider);
                throw new EmailAlreadyExistsException(userInfo.getEmail(), existingProvider);
            }
        }

        // Generate a temporary phone number (will be updated later if needed)
        String tempPhoneNumber = generateTempPhoneNumber(userInfo.getProvider(), userInfo.getProviderId());

        // Create new user
        User newUser = User.builder()
                .phoneNumber(tempPhoneNumber)
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .oauthProvider(userInfo.getProvider())
                .oauthId(userInfo.getProviderId())
                .userType(User.UserType.MASTER)
                .accountType(User.AccountType.SENIOR) // Default to SENIOR
                .birthDate(null) // Changed: don't assume age
                .gender(User.Gender.OTHER)
                .isActive(true)
                .consentPrivacy(true) // Assumed true for OAuth2
                .consentService(true) // Assumed true for OAuth2
                .consentMarketing(false) // Default false
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New OAuth2 user created: {} ({})",
                 savedUser.getEmail(), userInfo.getProvider());

        return savedUser;
    }

    private String generateTempPhoneNumber(String provider, String oauthId) {
        // Generate deterministic phone number from provider + oauthId
        // Hash space: 100 billion values (11 digits)
        String input = provider + "_" + oauthId;
        long hash = Integer.toUnsignedLong(input.hashCode());
        return String.format("oauth_%011d", hash % PHONE_NUMBER_HASH_SPACE);
    }

    /**
     * Generate access and refresh tokens for user
     */
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
}
