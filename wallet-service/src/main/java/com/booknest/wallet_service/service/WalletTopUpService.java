package com.booknest.wallet_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

import com.booknest.wallet_service.dto.CreateTopupOrderRequest;
import com.booknest.wallet_service.dto.CreateTopupOrderResponse;
import com.booknest.wallet_service.dto.VerifyTopupRequest;
import com.booknest.wallet_service.dto.VerifyTopupResponse;
import com.booknest.wallet_service.dto.WalletRequest;
import com.booknest.wallet_service.entity.Wallet;
import com.booknest.wallet_service.entity.WalletTransaction;
import com.booknest.wallet_service.entity.WalletTransactionStatus;
import com.booknest.wallet_service.repository.WalletTransactionRepository;

@Service
@Slf4j
public class WalletTopUpService {

    private static final double MIN_TOPUP_INR = 10.0;
    private static final double MAX_TOPUP_INR = 10_000.0;
    private static final double DAILY_LIMIT_INR = 50_000.0;
    private static final ZoneId TOPUP_ZONE = ZoneId.of("Asia/Kolkata");

    private final WalletTransactionRepository transactionRepository;
    private final WalletRazorpayService razorpayService;
    private final WalletService walletService;

    public WalletTopUpService(
            WalletTransactionRepository transactionRepository,
            WalletRazorpayService razorpayService,
            WalletService walletService) {
        this.transactionRepository = transactionRepository;
        this.razorpayService = razorpayService;
        this.walletService = walletService;
    }

    @Transactional
    public CreateTopupOrderResponse createTopupOrder(Long userId, CreateTopupOrderRequest body) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        if (body == null || body.getAmount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        double amount = body.getAmount();
        validateTopupAmount(amount);
        assertDailyLimit(userId, amount);

        String receipt = UUID.randomUUID().toString().replace("-", "");
        String orderId;
        try {
            orderId = razorpayService.createOrderInr(amount, receipt);
        } catch (IllegalStateException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay order creation", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create payment order");
        }

        LocalDateTime now = ZonedDateTime.now(TOPUP_ZONE).toLocalDateTime();
        WalletTransaction txn = new WalletTransaction();
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setRazorpayOrderId(orderId);
        txn.setStatus(WalletTransactionStatus.PENDING);
        txn.setCreatedAt(now);
        transactionRepository.save(txn);
        log.info("Top-up order created for userId={}, amount={}, orderId={}", userId, amount, orderId);

        return new CreateTopupOrderResponse(orderId, amount, razorpayService.getPublishableKey());
    }

    @Transactional
    public VerifyTopupResponse verifyTopup(Long userId, VerifyTopupRequest body) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        if (body == null
                || isBlank(body.getRazorpayOrderId())
                || isBlank(body.getRazorpayPaymentId())
                || isBlank(body.getRazorpaySignature())) {
            throw new IllegalArgumentException("razorpayOrderId, razorpayPaymentId, and razorpaySignature are required");
        }

        WalletTransaction txn = transactionRepository
                .findByRazorpayOrderIdAndUserIdForUpdate(body.getRazorpayOrderId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unknown top-up order"));

        if (txn.getStatus() == WalletTransactionStatus.SUCCESS) {
            Wallet w = walletService.getOrCreateWallet(userId);
            return new VerifyTopupResponse("Wallet updated successfully", w.getBalance());
        }
        if (txn.getStatus() == WalletTransactionStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This top-up cannot be completed");
        }

        boolean signatureOk;
        try {
            signatureOk = razorpayService.verifySignature(
                    body.getRazorpayOrderId(), body.getRazorpayPaymentId(), body.getRazorpaySignature());
        } catch (Exception e) {
            log.error("Payment verification failure for orderId={}: {}", body.getRazorpayOrderId(), e.getMessage());
            txn.setStatus(WalletTransactionStatus.FAILED);
            transactionRepository.save(txn);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment verification failed");
        }

        if (!signatureOk) {
            log.warn("Invalid signature for orderId={}, userId={}", body.getRazorpayOrderId(), userId);
            txn.setStatus(WalletTransactionStatus.FAILED);
            transactionRepository.save(txn);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
        }

        if (transactionRepository.existsByRazorpayPaymentIdAndStatus(body.getRazorpayPaymentId(), WalletTransactionStatus.SUCCESS)) {
            log.warn("Duplicate payment attempt: paymentId={} for orderId={}", body.getRazorpayPaymentId(), body.getRazorpayOrderId());
            txn.setStatus(WalletTransactionStatus.FAILED);
            transactionRepository.save(txn);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already used");
        }

        WalletRequest credit = new WalletRequest();
        credit.setUserId(userId);
        credit.setAmount(txn.getAmount());
        credit.setDescription("Wallet top-up (Razorpay)");
        walletService.addMoney(credit);

        txn.setRazorpayPaymentId(body.getRazorpayPaymentId());
        txn.setStatus(WalletTransactionStatus.SUCCESS);
        transactionRepository.save(txn);
        log.info("Top-up successful for userId={}, amount={}, orderId={}", userId, txn.getAmount(), body.getRazorpayOrderId());

        Wallet w = walletService.getOrCreateWallet(userId);
        return new VerifyTopupResponse("Wallet updated successfully", w.getBalance());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void validateTopupAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Invalid amount");
        }
        if (amount < MIN_TOPUP_INR) {
            throw new IllegalArgumentException("Minimum top-up amount is ₹10");
        }
        if (amount > MAX_TOPUP_INR) {
            throw new IllegalArgumentException("Maximum top-up amount is ₹10,000 per transaction");
        }
    }

    private void assertDailyLimit(Long userId, double additionalAmount) {
        LocalDateTime startOfDay = LocalDate.now(TOPUP_ZONE).atStartOfDay();
        Double used = transactionRepository.sumSuccessAmountSince(userId, WalletTransactionStatus.SUCCESS, startOfDay);
        double prior = used != null ? used : 0.0;
        if (prior + additionalAmount > DAILY_LIMIT_INR + 1e-6) {
            throw new IllegalArgumentException("Daily top-up limit of ₹50,000 would be exceeded");
        }
    }
}
