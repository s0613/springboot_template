package com.template.app.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAccountLoginRequest {

    @NotBlank(message = "전화번호를 입력해주세요")
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
            message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String masterPhoneNumber; // Master account's phone number

    @NotBlank(message = "로그인 코드를 입력해주세요")
    @Size(min = 6, max = 6, message = "로그인 코드는 6자리입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "로그인 코드는 숫자 6자리입니다")
    private String loginCode; // Sub-account's 6-digit login code
}
