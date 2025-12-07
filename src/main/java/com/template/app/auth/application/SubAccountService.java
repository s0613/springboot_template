package com.template.app.auth.application;

import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.api.dto.TokenResponse;
import com.template.app.auth.api.dto.request.CreateSubAccountRequest;
import com.template.app.auth.api.dto.request.SubAccountLoginRequest;
import com.template.app.auth.api.dto.response.SubAccountResponse;
import com.template.app.auth.domain.entity.User;
import com.template.app.auth.infrastructure.exception.InvalidCredentialsException;
import com.template.app.auth.infrastructure.exception.UserAlreadyExistsException;
import com.template.app.auth.infrastructure.exception.UserNotFoundException;
import com.template.app.auth.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubAccountService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_SUB_ACCOUNTS = 5; // Maximum sub-accounts per master

    /**
     * Create a new sub-account for the given master user
     *
     * @param masterUserId ID of the master user
     * @param request Sub-account creation request
     * @return Created sub-account response
     */
    @Transactional
    public SubAccountResponse createSubAccount(Long masterUserId, CreateSubAccountRequest request) {
        // Find master user
        User masterUser = userRepository.findById(masterUserId)
                .orElseThrow(() -> new UserNotFoundException("Master user not found"));

        // Verify it's a master account
        if (masterUser.getUserType() != User.UserType.MASTER) {
            throw new IllegalStateException("Only master accounts can create sub-accounts");
        }

        // Check sub-account limit
        long subAccountCount = userRepository.countByMasterUser(masterUser);
        if (subAccountCount >= MAX_SUB_ACCOUNTS) {
            throw new IllegalStateException(
                    String.format("Maximum sub-account limit (%d) reached", MAX_SUB_ACCOUNTS));
        }

        // Generate unique 6-digit login code
        String loginCode = generateUniqueLoginCode();

        // Create sub-account
        User subAccount = User.builder()
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .accountType(request.getAccountType())
                .userType(User.UserType.SUB_ACCOUNT)
                .masterUser(masterUser)
                .phoneNumber(masterUser.getPhoneNumber()) // Inherit master's phone number
                .loginCode(loginCode)
                // Inherit or use provided consent settings
                .consentPrivacy(request.getConsentPrivacy() != null ?
                        request.getConsentPrivacy() : masterUser.getConsentPrivacy())
                .consentService(request.getConsentService() != null ?
                        request.getConsentService() : masterUser.getConsentService())
                .consentMarketing(request.getConsentMarketing() != null ?
                        request.getConsentMarketing() : masterUser.getConsentMarketing())
                .isActive(true)
                .build();

        subAccount = userRepository.save(subAccount);
        log.info("Sub-account created: {} for master user: {}", subAccount.getId(), masterUserId);

        return SubAccountResponse.from(subAccount);
    }

    /**
     * Login with sub-account using master's phone number and login code
     *
     * @param request Sub-account login request
     * @return Token response
     */
    @Transactional
    public TokenResponse loginSubAccount(SubAccountLoginRequest request) {
        // Find sub-account by login code (login codes are globally unique)
        User subAccount = userRepository.findByLoginCode(request.getLoginCode())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login code"));

        // Verify the master user's phone number matches
        if (subAccount.getMasterUser() == null ||
                !subAccount.getMasterUser().getPhoneNumber().equals(request.getMasterPhoneNumber())) {
            throw new InvalidCredentialsException("Invalid login credentials");
        }

        // Check if sub-account is active
        if (!subAccount.getIsActive()) {
            throw new InvalidCredentialsException("Sub-account is deactivated");
        }

        // Update last login
        subAccount.updateLastLogin();
        userRepository.save(subAccount);

        log.info("Sub-account logged in: {} (master: {})",
                subAccount.getId(), subAccount.getMasterUser().getId());

        // Generate tokens
        return generateTokens(subAccount);
    }

    /**
     * Get all sub-accounts for a master user
     *
     * @param masterUserId ID of the master user
     * @return List of sub-accounts
     */
    @Transactional(readOnly = true)
    public List<SubAccountResponse> getSubAccounts(Long masterUserId) {
        User masterUser = userRepository.findById(masterUserId)
                .orElseThrow(() -> new UserNotFoundException("Master user not found"));

        List<User> subAccounts = userRepository.findByMasterUser(masterUser);

        return subAccounts.stream()
                .map(SubAccountResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific sub-account
     *
     * @param masterUserId ID of the master user
     * @param subAccountId ID of the sub-account
     * @return Sub-account response
     */
    @Transactional(readOnly = true)
    public SubAccountResponse getSubAccount(Long masterUserId, Long subAccountId) {
        User masterUser = userRepository.findById(masterUserId)
                .orElseThrow(() -> new UserNotFoundException("Master user not found"));

        User subAccount = userRepository.findById(subAccountId)
                .orElseThrow(() -> new UserNotFoundException("Sub-account not found"));

        // Verify ownership
        if (subAccount.getMasterUser() == null ||
                !subAccount.getMasterUser().getId().equals(masterUserId)) {
            throw new IllegalStateException("Sub-account does not belong to this master user");
        }

        return SubAccountResponse.from(subAccount);
    }

    /**
     * Delete a sub-account
     *
     * @param masterUserId ID of the master user
     * @param subAccountId ID of the sub-account to delete
     */
    @Transactional
    public void deleteSubAccount(Long masterUserId, Long subAccountId) {
        User masterUser = userRepository.findById(masterUserId)
                .orElseThrow(() -> new UserNotFoundException("Master user not found"));

        User subAccount = userRepository.findById(subAccountId)
                .orElseThrow(() -> new UserNotFoundException("Sub-account not found"));

        // Verify ownership
        if (subAccount.getMasterUser() == null ||
                !subAccount.getMasterUser().getId().equals(masterUserId)) {
            throw new IllegalStateException("Sub-account does not belong to this master user");
        }

        // Mark as deleted (soft delete)
        subAccount.markAsDeleted("관리자 삭제", "마스터 계정에 의한 하위 계정 삭제");
        userRepository.save(subAccount);

        log.info("Sub-account deleted: {} by master user: {}", subAccountId, masterUserId);
    }

    /**
     * Generate a unique 6-digit login code
     *
     * @return 6-digit login code as string
     */
    private String generateUniqueLoginCode() {
        String code;
        int maxAttempts = 100;
        int attempts = 0;

        do {
            // Generate 6-digit code (100000 to 999999)
            int codeInt = RANDOM.nextInt(900000) + 100000;
            code = String.valueOf(codeInt);

            attempts++;
            if (attempts >= maxAttempts) {
                throw new IllegalStateException("Failed to generate unique login code");
            }
        } while (userRepository.existsByLoginCode(code));

        return code;
    }

    /**
     * Generate tokens for sub-account
     *
     * @param user Sub-account user
     * @return Token response
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
