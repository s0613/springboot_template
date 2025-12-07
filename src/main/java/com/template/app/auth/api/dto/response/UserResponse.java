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
public class UserResponse {
    private Long id;
    private String phoneNumber;
    private String name;
    private LocalDate birthDate;
    private User.Gender gender;
    private String email;
    private User.UserType userType;
    private User.AccountType accountType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .email(user.getEmail())
                .userType(user.getUserType())
                .accountType(user.getAccountType())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
