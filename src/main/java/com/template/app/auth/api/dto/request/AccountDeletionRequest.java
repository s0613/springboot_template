package com.template.app.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequest {

    /**
     * Password for verification (required for password-based users, optional for OAuth users)
     */
    private String password;

    /**
     * User-selected reason for account deletion
     * Examples: "서비스 불만족", "사용 안 함", "개인정보 보호", "다른 서비스 이용", "기타"
     */
    @NotBlank(message = "탈퇴 사유를 선택해주세요")
    @Size(max = 50, message = "탈퇴 사유는 50자를 초과할 수 없습니다")
    private String reason;

    /**
     * Optional detailed feedback from user
     */
    @Size(max = 500, message = "상세 사유는 500자를 초과할 수 없습니다")
    private String detailReason;
}
