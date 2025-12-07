package com.template.app.payment.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for initiating a payment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Size(max = 100, message = "Order ID must not exceed 100 characters")
    private String orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum payment amount is 100")
    @DecimalMax(value = "10000000", message = "Maximum payment amount is 10,000,000")
    private BigDecimal amount;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String productName;

    @Size(max = 500, message = "Product description must not exceed 500 characters")
    private String productDescription;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String customerEmail;

    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;

    @Pattern(regexp = "^01[0-9]{8,9}$", message = "Invalid phone number format")
    private String customerPhone;

    /**
     * Success redirect URL after payment
     */
    private String successUrl;

    /**
     * Failure redirect URL after payment failure
     */
    private String failUrl;
}
