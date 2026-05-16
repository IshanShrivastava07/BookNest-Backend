package com.booknest.wallet_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booknest.wallet_service.entity.WalletTransaction;
import com.booknest.wallet_service.entity.WalletTransactionStatus;

import jakarta.persistence.LockModeType;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTransaction t WHERE t.razorpayOrderId = :orderId AND t.userId = :userId")
    Optional<WalletTransaction> findByRazorpayOrderIdAndUserIdForUpdate(
            @Param("orderId") String orderId,
            @Param("userId") Long userId);

    boolean existsByRazorpayPaymentIdAndStatus(String razorpayPaymentId, WalletTransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt >= :since")
    Double sumSuccessAmountSince(
            @Param("userId") Long userId,
            @Param("status") WalletTransactionStatus status,
            @Param("since") LocalDateTime since);
}
