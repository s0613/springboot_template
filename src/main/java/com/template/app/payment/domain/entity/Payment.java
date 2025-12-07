package com.template.app.payment.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity for storing payment transaction records.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_user_id", columnList = "user_id"),
    @Index(name = "idx_payments_order_id", columnList = "order_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_description", length = 500)
    private String productDescription;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Payment status enum
     */
    public enum PaymentStatus {
        PENDING,      // Payment initiated
        READY,        // Ready for payment (e.g., virtual account issued)
        IN_PROGRESS,  // Payment in progress
        DONE,         // Payment completed
        CANCELED,     // Payment cancelled
        PARTIAL_CANCELED, // Partially cancelled
        ABORTED,      // Payment aborted
        EXPIRED,      // Payment expired
        FAILED        // Payment failed
    }

    /**
     * Payment method enum
     */
    public enum PaymentMethod {
        CARD,           // Credit/Debit card
        VIRTUAL_ACCOUNT, // Virtual bank account
        EASY_PAY,       // Easy pay (Kakao Pay, Naver Pay, etc.)
        MOBILE,         // Mobile payment
        TRANSFER,       // Bank transfer
        CULTURE_GIFT,   // Culture gift card
        BOOK_GIFT,      // Book gift card
        GAME_GIFT       // Game gift card
    }

    /**
     * Mark payment as completed
     */
    public void markAsCompleted(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Mark payment as failed
     */
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Cancel payment
     */
    public void cancel(String reason, BigDecimal refundAmount) {
        this.status = refundAmount != null && refundAmount.compareTo(this.amount) < 0
            ? PaymentStatus.PARTIAL_CANCELED
            : PaymentStatus.CANCELED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        this.refundAmount = refundAmount != null ? refundAmount : this.amount;
    }
}
