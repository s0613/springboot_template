package com.template.app.payment.infrastructure.repository;

import com.template.app.payment.domain.entity.Payment;
import com.template.app.payment.domain.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Find payment by payment key
     */
    Optional<Payment> findByPaymentKey(String paymentKey);

    /**
     * Find all payments for a user
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find payments by user ID and status
     */
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find pending payments older than specified time (for cleanup)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoff")
    List<Payment> findStalePendingPayments(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count payments by user ID and status within date range
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.userId = :userId AND p.status = :status " +
           "AND p.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndStatusAndDateRange(
        @Param("userId") Long userId,
        @Param("status") PaymentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get total amount paid by user
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.userId = :userId AND p.status = 'DONE'")
    java.math.BigDecimal getTotalAmountByUserId(@Param("userId") Long userId);
}
