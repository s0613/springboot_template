package com.template.app.auth.api.controller;

import com.template.app.auth.api.dto.request.AccountDeletionRequest;
import com.template.app.auth.api.dto.response.AccountDeletionResponse;
import com.template.app.auth.application.AccountDeletionService;
import com.template.app.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "계정 관리", description = "사용자 계정의 탈퇴와 복구를 처리하는 API입니다. 탈퇴 시 14일간의 복구 기간이 제공되며, 이 기간 내에 언제든지 계정을 복구할 수 있습니다. 복구 기간이 지나면 모든 데이터가 영구 삭제됩니다.")
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    /**
     * Delete (deactivate) user account
     * DELETE /api/v1/users/me
     *
     * @param userIdStr Authenticated user ID from JWT token (as String)
     * @param request Account deletion request with password and reason
     * @param httpRequest HTTP request to extract access token
     * @return Account deletion response
     */
    @DeleteMapping("/me")
    @Operation(
            summary = "계정 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴(비활성화) 처리합니다. " +
                    "⚠️ 주의사항: 탈퇴 처리 시 현재 사용 중인 Access Token이 즉시 블랙리스트에 등록되어 무효화됩니다. " +
                    "탈퇴 후 14일간 복구 기간이 제공되며, 이 기간 동안에는 로그인 시도 시 '탈퇴된 계정입니다. 복구하시겠습니까?' 안내가 표시됩니다. " +
                    "14일이 경과하면 계정 및 모든 관련 데이터(평가 기록, 피드백 리포트, 서브계정 등)가 영구적으로 삭제되며 복구가 불가능합니다. " +
                    "탈퇴 처리를 위해서는 현재 비밀번호 확인이 필수이며, 서비스 개선을 위한 탈퇴 사유 선택이 요청됩니다(선택사항)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 불일치 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> deleteAccount(
            @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody AccountDeletionRequest request,
            HttpServletRequest httpRequest) {

        Long userId = Long.parseLong(userIdStr);
        log.info("Account deletion request received for user: {}", userId);

        // Extract access token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // Process account deletion
        AccountDeletionResponse response = accountDeletionService.deleteAccount(
                userId,
                request,
                accessToken
        );

        log.info("Account deletion completed for user: {}", userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Recover deleted account within 14-day recovery period
     * POST /api/v1/users/me/recover
     *
     * @param userIdStr Authenticated user ID from JWT token (as String)
     * @return Success response
     */
    @PostMapping("/me/recover")
    @Operation(
            summary = "계정 복구",
            description = "탈퇴 처리된 계정을 복구하여 다시 활성화합니다. " +
                    "복구 가능 조건: 탈퇴일로부터 14일 이내여야 합니다. " +
                    "복구 시 이전 계정의 모든 데이터가 그대로 유지됩니다(평가 기록, 피드백 리포트, 서브계정, 프로필 정보 등). " +
                    "복구 후 새로운 Access Token과 Refresh Token이 발급되어 정상적인 서비스 이용이 가능합니다. " +
                    "14일이 경과한 계정은 이미 데이터가 영구 삭제되어 복구가 불가능하며, 새로 회원가입이 필요합니다. " +
                    "탈퇴 상태에서 로그인 시도 시 복구 안내 화면이 표시되며, 해당 화면에서 이 API를 호출하여 복구를 진행합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "계정 복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "복구 기간 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> recoverAccount(
            @AuthenticationPrincipal String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        log.info("Account recovery request received for user: {}", userId);

        String message = accountDeletionService.recoverAccount(userId);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("userId", userId.toString());
        responseData.put("message", message);

        log.info("Account recovery completed for user: {}", userId);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}
