package com.booknest.wallet_service.service;

import com.booknest.wallet_service.dto.*;
import com.booknest.wallet_service.entity.*;
import com.booknest.wallet_service.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTopUpServiceTest {

    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private WalletRazorpayService razorpayService;
    @Mock private WalletService walletService;

    @InjectMocks
    private WalletTopUpService topUpService;

    private Long userId = 1L;
    private CreateTopupOrderRequest createRequest;
    private VerifyTopupRequest verifyRequest;
    private WalletTransaction transaction;

    @BeforeEach
    void setUp() {
        createRequest = new CreateTopupOrderRequest();
        createRequest.setAmount(100.0);

        verifyRequest = new VerifyTopupRequest();
        verifyRequest.setRazorpayOrderId("order_123");
        verifyRequest.setRazorpayPaymentId("pay_123");
        verifyRequest.setRazorpaySignature("sig_123");

        transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(100.0);
        transaction.setRazorpayOrderId("order_123");
        transaction.setStatus(WalletTransactionStatus.PENDING);
    }

    @Test
    void createTopupOrder_Unauthorized_ThrowsResponseStatusException() {
        assertThrows(ResponseStatusException.class, () -> topUpService.createTopupOrder(null, createRequest));
    }

    @Test
    void createTopupOrder_InvalidAmount_ThrowsIllegalArgumentException() {
        createRequest.setAmount(5.0); // MIN is 10
        assertThrows(IllegalArgumentException.class, () -> topUpService.createTopupOrder(userId, createRequest));
    }

    @Test
    void createTopupOrder_RazorpayError_ThrowsResponseStatusException() throws Exception {
        when(razorpayService.createOrderInr(anyDouble(), anyString())).thenThrow(new IllegalStateException("Razorpay down"));
        assertThrows(ResponseStatusException.class, () -> topUpService.createTopupOrder(userId, createRequest));
    }

    @Test
    void createTopupOrder_GenericError_ThrowsResponseStatusException() throws Exception {
        when(razorpayService.createOrderInr(anyDouble(), anyString())).thenThrow(new RuntimeException("Error"));
        assertThrows(ResponseStatusException.class, () -> topUpService.createTopupOrder(userId, createRequest));
    }

    @Test
    void createTopupOrder_DailyLimitExceeded_ThrowsIllegalArgumentException() {
        when(transactionRepository.sumSuccessAmountSince(anyLong(), any(), any())).thenReturn(49950.0);
        createRequest.setAmount(100.0); // Total 50050 > 50000
        assertThrows(IllegalArgumentException.class, () -> topUpService.createTopupOrder(userId, createRequest));
    }

    @Test
    void verifyTopup_IdempotentSuccess_ReturnsBalance() {
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));
        when(walletService.getOrCreateWallet(userId)).thenReturn(new Wallet());

        VerifyTopupResponse response = topUpService.verifyTopup(userId, verifyRequest);
        assertEquals("Wallet updated successfully", response.getMessage());
    }

    @Test
    void verifyTopup_FailedTransaction_ThrowsResponseStatusException() {
        transaction.setStatus(WalletTransactionStatus.FAILED);
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));

        assertThrows(ResponseStatusException.class, () -> topUpService.verifyTopup(userId, verifyRequest));
    }

    @Test
    void verifyTopup_VerificationException_MarksAsFailed() throws Exception {
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Verification Error"));

        assertThrows(ResponseStatusException.class, () -> topUpService.verifyTopup(userId, verifyRequest));
        assertEquals(WalletTransactionStatus.FAILED, transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void verifyTopup_InvalidSignature_MarksAsFailed() throws Exception {
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> topUpService.verifyTopup(userId, verifyRequest));
        assertEquals(WalletTransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    void verifyTopup_DuplicatePayment_MarksAsFailed() throws Exception {
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);
        when(transactionRepository.existsByRazorpayPaymentIdAndStatus(anyString(), eq(WalletTransactionStatus.SUCCESS))).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> topUpService.verifyTopup(userId, verifyRequest));
        assertEquals(WalletTransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    void verifyTopup_Success_UpdatesWalletAndTransaction() throws Exception {
        when(transactionRepository.findByRazorpayOrderIdAndUserIdForUpdate(anyString(), anyLong())).thenReturn(Optional.of(transaction));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);
        when(transactionRepository.existsByRazorpayPaymentIdAndStatus(anyString(), eq(WalletTransactionStatus.SUCCESS))).thenReturn(false);
        when(walletService.getOrCreateWallet(userId)).thenReturn(new Wallet());

        VerifyTopupResponse response = topUpService.verifyTopup(userId, verifyRequest);
        assertEquals(WalletTransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals("pay_123", transaction.getRazorpayPaymentId());
        verify(walletService).addMoney(any());
        verify(transactionRepository).save(transaction);
    }
}
