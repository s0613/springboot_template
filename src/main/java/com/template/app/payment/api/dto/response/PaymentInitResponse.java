package com.template.app.payment.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for payment initialization.
 * Contains information needed to proceed with PG payment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {

    private String orderId;
    private BigDecimal amount;
    private String productName;
    private String customerEmail;
    private String customerName;
    private String successUrl;
    private String failUrl;

    /**
     * Client key for PG integration (Toss Payments, etc.)
     */
    private String clientKey;
}
