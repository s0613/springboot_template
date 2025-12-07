package com.template.app.payment.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for confirming a payment after PG callback.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "Payment key is required")
    private String paymentKey;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
