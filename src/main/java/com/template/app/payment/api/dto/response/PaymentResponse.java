package com.template.app.payment.api.dto.response;

import com.template.app.payment.domain.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String orderId;
    private String paymentKey;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String productName;
    private String productDescription;
    private String customerEmail;
    private String customerName;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;

    /**
     * Factory method to create from Payment entity
     */
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .paymentKey(payment.getPaymentKey())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
            .productName(payment.getProductName())
            .productDescription(payment.getProductDescription())
            .customerEmail(payment.getCustomerEmail())
            .customerName(payment.getCustomerName())
            .approvedAt(payment.getApprovedAt())
            .cancelledAt(payment.getCancelledAt())
            .cancelReason(payment.getCancelReason())
            .refundAmount(payment.getRefundAmount())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
