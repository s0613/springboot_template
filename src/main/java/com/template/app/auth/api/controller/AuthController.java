package com.template.app.auth.api.controller;

import com.template.app.auth.infrastructure.security.JwtTokenProvider;
import com.template.app.auth.infrastructure.security.LoginAttemptService;
import com.template.app.auth.application.RefreshTokenService;
import com.template.app.auth.api.dto.TokenResponse;
import com.template.app.auth.api.dto.request.*;
import com.template.app.auth.api.dto.request.AppleTokenVerificationRequest;
import com.template.app.auth.api.dto.response.SubAccountResponse;
import com.template.app.auth.api.dto.response.UserResponse;
import com.template.app.auth.application.AuthService;
import com.template.app.auth.application.NativeOAuth2Service;
import com.template.app.auth.application.SmsVerificationService;
import com.template.app.auth.application.SubAccountService;
import com.template.app.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 관리, SMS 인증, 소셜 로그인 등 사용자 인증과 관련된 모든 API를 제공합니다.")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SmsVerificationService smsVerificationService;
    private final SubAccountService subAccountService;
    private final NativeOAuth2Service nativeOAuth2Service;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입",
               description = "새로운 사용자 계정을 생성합니다. " +
                       "필수 입력 항목: 전화번호(SMS 인증 완료 필수), 비밀번호(8자 이상, 영문/숫자/특수문자 조합), 이름, 생년월일. " +
                       "선택 입력 항목: 성별, 마케팅 수신 동의. " +
                       "회원가입 성공 시 Access Token(24시간)과 Refresh Token(30일)이 즉시 발급되어 로그인 상태가 됩니다.")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        TokenResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인",
               description = "전화번호와 비밀번호로 로그인합니다. " +
                       "로그인 성공 시 Access Token(유효기간 24시간)과 Refresh Token(유효기간 30일)이 발급됩니다. " +
                       "5회 연속 로그인 실패 시 계정이 일시적으로 잠기며, 30분 후 자동 해제됩니다. " +
                       "탈퇴 처리된 계정은 14일 이내 복구 가능하며, 복구 전까지 로그인이 제한됩니다.")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃",
               description = "현재 세션을 종료하고 사용 중인 Access Token을 블랙리스트에 등록하여 무효화합니다. " +
                       "로그아웃 후에는 해당 토큰으로 API 호출이 불가능하며, 다시 로그인해야 합니다. " +
                       "Refresh Token은 별도로 무효화되지 않으므로, 보안이 필요한 경우 클라이언트에서 삭제해야 합니다.")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신",
               description = "만료된 Access Token을 갱신합니다. " +
                       "유효한 Refresh Token을 전송하면 새로운 Access Token과 Refresh Token 쌍이 발급됩니다(Token Rotation). " +
                       "기존 Refresh Token은 즉시 무효화되어 재사용이 불가능합니다. " +
                       "Refresh Token이 만료되었거나 유효하지 않은 경우 다시 로그인해야 합니다.")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        TokenResponse response = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회",
               description = "현재 로그인한 사용자의 프로필 정보를 조회합니다. " +
                       "반환 정보: 사용자 ID, 이름, 전화번호, 생년월일, 성별, 역할(USER/CAREGIVER), 계정 생성일, 마지막 로그인 시간. " +
                       "서브계정으로 로그인한 경우 해당 서브계정의 정보가 반환됩니다.")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        UserResponse response = authService.getUserById(Long.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verification/send")
    @Operation(summary = "SMS 인증번호 발송",
               description = "입력한 전화번호로 6자리 인증번호가 포함된 SMS를 발송합니다. " +
                       "인증번호 유효시간: 5분. 동일 번호로 1분 이내 재발송 불가(스팸 방지). " +
                       "일일 발송 한도: 동일 번호 10회, 동일 IP 30회. " +
                       "회원가입 전 본인 인증 및 비밀번호 찾기에 사용됩니다.")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(
            @Valid @RequestBody SendVerificationRequest request) {

        boolean sent = smsVerificationService.sendVerificationCode(request.getPhoneNumber());

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(
                    "인증번호가 발송되었습니다. 5분 내에 입력해주세요."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.failure("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @PostMapping("/verification/verify")
    @Operation(summary = "SMS 인증번호 확인",
               description = "발송된 6자리 인증번호가 일치하는지 확인합니다. " +
                       "인증 성공 시 해당 전화번호로 회원가입이 가능해집니다(인증 완료 상태 10분간 유지). " +
                       "5회 연속 실패 시 해당 인증번호가 무효화되며, 새로운 인증번호를 발송받아야 합니다. " +
                       "인증번호 유효시간(5분)이 지나면 만료되어 재발송이 필요합니다.")
    public ResponseEntity<ApiResponse<String>> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {

        boolean verified = smsVerificationService.verifyCode(
                request.getPhoneNumber(),
                request.getCode()
        );

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success(
                    "전화번호 인증이 완료되었습니다."
            ));
        } else {
            long remainingTime = smsVerificationService.getRemainingTime(request.getPhoneNumber());

            if (remainingTime < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("인증번호가 만료되었습니다. 다시 요청해주세요."));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("인증번호가 일치하지 않습니다."));
            }
        }
    }

    // Sub-account endpoints
    @PostMapping("/sub-accounts")
    @Operation(summary = "서브계정 생성",
               description = "마스터 계정(보호자) 하위에 새로운 서브계정(피보호자/어르신)을 생성합니다. " +
                       "필수 입력: 이름, 생년월일. 선택 입력: 성별. " +
                       "서브계정 생성 시 6자리 숫자 PIN 코드가 자동 생성되며, 이 PIN으로 서브계정 전용 로그인이 가능합니다. " +
                       "마스터 계정당 최대 10개의 서브계정 생성 가능. 서브계정은 인지평가 수행 및 결과 조회만 가능합니다.")
    public ResponseEntity<SubAccountResponse> createSubAccount(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateSubAccountRequest request) {
        String token = extractToken(authHeader);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        SubAccountResponse response = subAccountService.createSubAccount(Long.valueOf(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sub-accounts")
    @Operation(summary = "서브계정 목록 조회",
               description = "현재 마스터 계정에 연결된 모든 서브계정(피보호자) 목록을 조회합니다. " +
                       "각 서브계정의 ID, 이름, 생년월일, 성별, PIN 코드, 생성일, 마지막 평가일 정보가 포함됩니다. " +
                       "목록은 생성일 기준 최신순으로 정렬됩니다.")
    public ResponseEntity<List<SubAccountResponse>> getSubAccounts(
            @RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        List<SubAccountResponse> response = subAccountService.getSubAccounts(Long.valueOf(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sub-accounts/{subAccountId}")
    @Operation(summary = "서브계정 상세 조회",
               description = "특정 서브계정의 상세 정보를 조회합니다. " +
                       "반환 정보: 서브계정 ID, 이름, 생년월일, 성별, 6자리 PIN 코드, 생성일, 마지막 평가일, 총 평가 횟수. " +
                       "본인 소유의 서브계정만 조회 가능합니다.")
    public ResponseEntity<SubAccountResponse> getSubAccount(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long subAccountId) {
        String token = extractToken(authHeader);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        SubAccountResponse response = subAccountService.getSubAccount(Long.valueOf(userId), subAccountId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sub-accounts/{subAccountId}")
    @Operation(summary = "서브계정 삭제",
               description = "특정 서브계정을 삭제합니다. " +
                       "⚠️ 주의: 삭제 시 해당 서브계정의 모든 평가 기록, 피드백 리포트가 함께 삭제되며 복구가 불가능합니다. " +
                       "본인 소유의 서브계정만 삭제 가능합니다.")
    public ResponseEntity<Void> deleteSubAccount(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long subAccountId) {
        String token = extractToken(authHeader);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        subAccountService.deleteSubAccount(Long.valueOf(userId), subAccountId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sub-accounts/login")
    @Operation(summary = "서브계정 로그인",
               description = "서브계정(피보호자) 전용 로그인입니다. " +
                       "마스터 계정의 전화번호와 서브계정 생성 시 발급된 6자리 PIN 코드를 입력합니다. " +
                       "서브계정으로 로그인하면 해당 서브계정의 인지평가 수행 및 본인 결과 조회만 가능하며, " +
                       "다른 서브계정 정보나 마스터 계정 기능에는 접근할 수 없습니다.")
    public ResponseEntity<TokenResponse> subAccountLogin(@Valid @RequestBody SubAccountLoginRequest request) {
        TokenResponse response = subAccountService.loginSubAccount(request);
        return ResponseEntity.ok(response);
    }

    // Native OAuth2 endpoints
    @PostMapping("/oauth2/google/verify")
    @Operation(summary = "Google 소셜 로그인",
               description = "iOS/Android 네이티브 앱에서 Google Sign-In SDK를 통해 받은 ID Token을 서버에서 검증합니다. " +
                       "Google 서버와 통신하여 토큰의 유효성과 사용자 정보(이메일, 이름, 프로필 사진)를 확인한 후, " +
                       "신규 사용자인 경우 자동으로 계정을 생성하고, 기존 사용자인 경우 로그인 처리합니다. " +
                       "성공 시 서비스용 JWT Access Token과 Refresh Token이 발급됩니다.")
    public ResponseEntity<TokenResponse> verifyGoogleToken(
            @Valid @RequestBody GoogleTokenVerificationRequest request) {
        TokenResponse response = nativeOAuth2Service.authenticateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth2/kakao/verify")
    @Operation(summary = "카카오 소셜 로그인",
               description = "iOS/Android 네이티브 앱에서 카카오 SDK를 통해 받은 Access Token을 서버에서 검증합니다. " +
                       "카카오 API 서버와 통신하여 사용자 정보(카카오 ID, 닉네임, 프로필 이미지, 이메일)를 조회한 후, " +
                       "신규 사용자인 경우 자동으로 계정을 생성하고, 기존 사용자인 경우 로그인 처리합니다. " +
                       "성공 시 서비스용 JWT Access Token과 Refresh Token이 발급됩니다.")
    public ResponseEntity<TokenResponse> verifyKakaoToken(
            @Valid @RequestBody KakaoTokenVerificationRequest request) {
        TokenResponse response = nativeOAuth2Service.authenticateWithKakao(request.getAccessToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth2/apple/verify")
    @Operation(summary = "Apple 소셜 로그인",
               description = "iOS 앱에서 Sign in with Apple을 통해 받은 ID Token을 서버에서 검증합니다. " +
                       "Apple 서버의 공개키로 ID Token의 서명을 검증하고 사용자 정보(Apple User ID, 이메일, 이름)를 추출합니다. " +
                       "⚠️ Apple은 최초 로그인 시에만 이메일과 이름을 제공하므로, 앱에서 이 정보를 함께 전송해야 합니다. " +
                       "신규 사용자인 경우 자동으로 계정을 생성하고, 기존 사용자인 경우 로그인 처리합니다.")
    public ResponseEntity<TokenResponse> verifyAppleToken(
            @Valid @RequestBody AppleTokenVerificationRequest request) {
        TokenResponse response = nativeOAuth2Service.authenticateWithApple(
                request.getIdToken(),
                request.getName(),
                request.getEmail()
        );
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }

    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    // Admin endpoint to reset login attempts (temporary for debugging)
    @DeleteMapping("/admin/login-attempts/{phoneNumber}")
    @Operation(summary = "[관리자] 로그인 시도 횟수 초기화",
               description = "특정 전화번호의 로그인 시도 횟수를 초기화하여 계정 잠금을 해제합니다.")
    public ResponseEntity<Void> resetLoginAttempts(@PathVariable String phoneNumber) {
        loginAttemptService.resetAttempts(phoneNumber);
        log.info("Reset login attempts for phone: {}", phoneNumber);
        return ResponseEntity.noContent().build();
    }
}
