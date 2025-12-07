package com.template.app.payment.service;

import com.template.app.payment.api.dto.request.PaymentCancelRequest;
import com.template.app.payment.api.dto.request.PaymentConfirmRequest;
import com.template.app.payment.api.dto.request.PaymentRequest;
import com.template.app.payment.api.dto.response.PaymentInitResponse;
import com.template.app.payment.api.dto.response.PaymentResponse;
import com.template.app.payment.domain.entity.Payment;
import com.template.app.payment.domain.entity.Payment.PaymentStatus;
import com.template.app.payment.infrastructure.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for handling payment operations.
 * This is a template implementation - integrate with your PG provider (Toss, Iamport, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${payment.pg.client-key:}")
    private String pgClientKey;

    @Value("${payment.pg.secret-key:}")
    private String pgSecretKey;

    @Value("${payment.success-url:}")
    private String defaultSuccessUrl;

    @Value("${payment.fail-url:}")
    private String defaultFailUrl;

    /**
     * Initialize a new payment.
     *
     * @param userId  User ID initiating the payment
     * @param request Payment request details
     * @return Payment initialization response for PG
     */
    @Transactional
    public PaymentInitResponse initiatePayment(Long userId, PaymentRequest request) {
        log.info("Initiating payment for user {} with order ID {}", userId, request.getOrderId());

        // Check for duplicate order ID
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalArgumentException("Order ID already exists: " + request.getOrderId());
        }

        // Create payment record
        Payment payment = Payment.builder()
            .userId(userId)
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .productName(request.getProductName())
            .productDescription(request.getProductDescription())
            .customerEmail(request.getCustomerEmail())
            .customerName(request.getCustomerName())
            .customerPhone(request.getCustomerPhone())
            .status(PaymentStatus.PENDING)
            .build();

        paymentRepository.save(payment);
        log.info("Payment record created with ID {} for order {}", payment.getId(), request.getOrderId());

        return PaymentInitResponse.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .productName(request.getProductName())
            .customerEmail(request.getCustomerEmail())
            .customerName(request.getCustomerName())
            .successUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : defaultSuccessUrl)
            .failUrl(request.getFailUrl() != null ? request.getFailUrl() : defaultFailUrl)
            .clientKey(pgClientKey)
            .build();
    }

    /**
     * Confirm a payment after PG callback.
     * This should call your PG provider's confirm API.
     *
     * @param request Payment confirmation request from PG callback
     * @return Payment response
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        log.info("Confirming payment for order {} with payment key {}", request.getOrderId(), request.getPaymentKey());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + request.getOrderId()));

        // Validate amount
        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            payment.markAsFailed("Amount mismatch");
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        // TODO: Call PG provider's confirm API here
        // Example for Toss Payments:
        // TossPaymentsConfirmResponse response = tossPaymentsClient.confirmPayment(request);

        // For now, mark as completed
        payment.markAsCompleted(request.getPaymentKey());
        paymentRepository.save(payment);

        log.info("Payment confirmed for order {}", request.getOrderId());
        return PaymentResponse.from(payment);
    }

    /**
     * Cancel a payment.
     *
     * @param paymentId Payment ID to cancel
     * @param request   Cancel request details
     * @return Payment response
     */
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId, PaymentCancelRequest request) {
        log.info("Cancelling payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.DONE) {
            throw new IllegalStateException("Cannot cancel payment with status: " + payment.getStatus());
        }

        // Validate refund amount
        BigDecimal refundAmount = request.getRefundAmount() != null
            ? request.getRefundAmount()
            : payment.getAmount();

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds payment amount");
        }

        // TODO: Call PG provider's cancel API here
        // Example for Toss Payments:
        // TossPaymentsCancelResponse response = tossPaymentsClient.cancelPayment(payment.getPaymentKey(), request);

        payment.cancel(request.getCancelReason(), refundAmount);
        paymentRepository.save(payment);

        log.info("Payment {} cancelled with refund amount {}", paymentId, refundAmount);
        return PaymentResponse.from(payment);
    }

    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return PaymentResponse.from(payment);
    }

    /**
     * Get payment by order ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + orderId));
        return PaymentResponse.from(payment);
    }

    /**
     * Get user's payment history.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(PaymentResponse::from);
    }

    /**
     * Get user's total paid amount.
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserTotalPaidAmount(Long userId) {
        return paymentRepository.getTotalAmountByUserId(userId);
    }

    /**
     * Handle payment failure from PG.
     */
    @Transactional
    public PaymentResponse handlePaymentFailure(String orderId, String reason) {
        log.warn("Payment failed for order {}: {}", orderId, reason);

        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + orderId));

        payment.markAsFailed(reason);
        paymentRepository.save(payment);

        return PaymentResponse.from(payment);
    }
}
