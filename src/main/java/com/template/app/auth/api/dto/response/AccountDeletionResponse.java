package com.template.app.auth.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionResponse {

    /**
     * User ID of the deleted account
     */
    private String userId;

    /**
     * Timestamp when the account was deleted
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    /**
     * Message to display to the user
     */
    private String message;

    /**
     * Number of days until permanent deletion
     */
    private Integer recoveryPeriodDays;

    /**
     * Expiry date for account recovery
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recoveryExpiresAt;
}
