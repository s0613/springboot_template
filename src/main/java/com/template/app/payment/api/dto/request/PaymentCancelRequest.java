package com.template.app.payment.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for cancelling a payment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {

    @NotBlank(message = "Cancel reason is required")
    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    private String cancelReason;

    /**
     * Partial refund amount (optional).
     * If not specified, full refund is processed.
     */
    @DecimalMin(value = "0", inclusive = false, message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;
}
