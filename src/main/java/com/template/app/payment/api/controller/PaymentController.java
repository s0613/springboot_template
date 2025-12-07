package com.template.app.payment.api.controller;

import com.template.app.common.dto.ApiResponse;
import com.template.app.payment.api.dto.request.PaymentCancelRequest;
import com.template.app.payment.api.dto.request.PaymentConfirmRequest;
import com.template.app.payment.api.dto.request.PaymentRequest;
import com.template.app.payment.api.dto.response.PaymentInitResponse;
import com.template.app.payment.api.dto.response.PaymentResponse;
import com.template.app.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for payment operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initialize a new payment.
     * Returns information needed for PG integration.
     */
    @PostMapping("/init")
    @Operation(summary = "Initialize payment", description = "Create a payment record and return PG integration data")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initiatePayment(
            @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody PaymentRequest request) {

        Long userId = Long.parseLong(userIdStr);
        log.info("Initiating payment for user {} with order ID {}", userId, request.getOrderId());

        PaymentInitResponse response = paymentService.initiatePayment(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Confirm payment after PG callback.
     * This endpoint is called after the user completes payment on PG page.
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment", description = "Confirm payment after PG callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request) {

        log.info("Confirming payment for order {}", request.getOrderId());

        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment", description = "Get payment details by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long paymentId) {

        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get payment by order ID.
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Get payment details by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable String orderId) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current user's payment history.
     */
    @GetMapping("/my")
    @Operation(summary = "Get my payments", description = "Get current user's payment history")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal String userIdStr,
            @PageableDefault(size = 20) Pageable pageable) {

        Long userId = Long.parseLong(userIdStr);
        Page<PaymentResponse> response = paymentService.getUserPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current user's total paid amount.
     */
    @GetMapping("/my/total")
    @Operation(summary = "Get my total paid amount", description = "Get current user's total paid amount")
    public ResponseEntity<ApiResponse<BigDecimal>> getMyTotalPaidAmount(
            @AuthenticationPrincipal String userIdStr) {

        Long userId = Long.parseLong(userIdStr);
        BigDecimal total = paymentService.getUserTotalPaidAmount(userId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    /**
     * Cancel a payment.
     */
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancel a completed payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {

        log.info("Cancelling payment {}", paymentId);

        PaymentResponse response = paymentService.cancelPayment(paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Handle payment failure callback from PG.
     */
    @PostMapping("/fail")
    @Operation(summary = "Handle payment failure", description = "Handle payment failure callback from PG")
    public ResponseEntity<ApiResponse<PaymentResponse>> handlePaymentFailure(
            @RequestParam String orderId,
            @RequestParam String message) {

        log.warn("Payment failure for order {}: {}", orderId, message);

        PaymentResponse response = paymentService.handlePaymentFailure(orderId, message);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
