package com.template.app.auth.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date", nullable = true)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "consent_privacy", nullable = false)
    @Builder.Default
    private Boolean consentPrivacy = false;

    @Column(name = "consent_service", nullable = false)
    @Builder.Default
    private Boolean consentService = false;

    @Column(name = "consent_marketing", nullable = false)
    @Builder.Default
    private Boolean consentMarketing = false;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", length = 255, unique = true)
    private String email;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    @Builder.Default
    private UserType userType = UserType.MASTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.SENIOR;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deletion_reason", length = 50)
    private String deletionReason;

    @Column(name = "detailed_deletion_reason", length = 500)
    private String detailedDeletionReason;

    // Sub-account fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User masterUser;

    @Column(name = "login_code", length = 6)
    private String loginCode; // 6-digit code for sub-accounts

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum UserType {
        MASTER, SUB_ACCOUNT
    }

    public enum AccountType {
        SENIOR, CAREGIVER, ADMIN
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isOAuthUser() {
        return oauthProvider != null && oauthId != null;
    }

    public boolean isPasswordUser() {
        return password != null;
    }

    public boolean isDeleted() {
        return !isActive && deletedAt != null;
    }

    public void markAsDeleted(String reason, String detailReason) {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
        this.deletionReason = reason;
        this.detailedDeletionReason = detailReason;
    }

    public void restoreAccount() {
        this.isActive = true;
        this.deletedAt = null;
        this.deletionReason = null;
        this.detailedDeletionReason = null;
    }
}
