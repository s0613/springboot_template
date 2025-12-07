package com.template.app.auth.api.dto.response;

import com.template.app.auth.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAccountResponse {

    private Long id;
    private String name;
    private LocalDate birthDate;
    private User.Gender gender;
    private User.AccountType accountType;
    private String loginCode; // 6-digit login code
    private Long masterUserId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Boolean isActive;

    public static SubAccountResponse from(User user) {
        return SubAccountResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .loginCode(user.getLoginCode())
                .masterUserId(user.getMasterUser() != null ? user.getMasterUser().getId() : null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .isActive(user.getIsActive())
                .build();
    }
}
