package com.template.app.auth.api.dto.request;

import com.template.app.auth.domain.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubAccountRequest {

    @NotBlank(message = "이름을 입력해주세요")
    @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하여야 합니다")
    private String name;

    @NotNull(message = "생년월일을 입력해주세요")
    private LocalDate birthDate;

    @NotNull(message = "성별을 선택해주세요")
    private User.Gender gender;

    @NotNull(message = "계정 유형을 선택해주세요")
    private User.AccountType accountType;

    // Privacy consents (optional for sub-accounts, inherit from master)
    private Boolean consentPrivacy;
    private Boolean consentService;
    private Boolean consentMarketing;
}
