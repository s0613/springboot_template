package com.template.app.auth.api.dto.request;

import com.template.app.auth.domain.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotNull(message = "Gender is required")
    private User.Gender gender;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Privacy consent is required")
    @AssertTrue(message = "Privacy consent must be accepted")
    private Boolean consentPrivacy;

    @NotNull(message = "Service consent is required")
    @AssertTrue(message = "Service consent must be accepted")
    private Boolean consentService;

    @Builder.Default
    private Boolean consentMarketing = false;

    @NotNull(message = "Account type is required")
    private User.AccountType accountType;
}
