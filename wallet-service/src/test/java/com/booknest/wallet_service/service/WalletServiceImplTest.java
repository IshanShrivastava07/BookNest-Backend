package com.booknest.wallet_service.service;

import com.booknest.wallet_service.dto.WalletRequest;
import com.booknest.wallet_service.entity.Statement;
import com.booknest.wallet_service.entity.Wallet;
import com.booknest.wallet_service.messaging.WalletEventPublisher;
import com.booknest.wallet_service.repository.StatementRepository;
import com.booknest.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private WalletRepository walletRepo;
    @Mock private StatementRepository statementRepo;
    @Mock private WalletEventPublisher walletEventPublisher;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setUserId(10L);
        wallet.setBalance(500.0);
    }

    @Test
    void createWallet_NullUserId_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(null));
    }

    @Test
    void createWallet_ValidUser_ShouldSave() {
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);
        Wallet result = walletService.createWallet(10L);
        assertEquals(wallet, result);
        verify(walletRepo).save(any(Wallet.class));
    }

    @Test
    void getOrCreateWallet_WalletExists_ShouldReturn() {
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        Wallet result = walletService.getOrCreateWallet(10L);
        assertEquals(wallet, result);
        verify(walletRepo, never()).save(any());
    }

    @Test
    void getOrCreateWallet_WalletMissing_ShouldCreate() {
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.empty());
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);
        Wallet result = walletService.getOrCreateWallet(10L);
        assertEquals(wallet, result);
        verify(walletRepo).save(any(Wallet.class));
    }

    @Test
    void getOrCreateWallet_DuplicateKeyRace_ShouldFallbackToFind() {
        when(walletRepo.findByUserId(10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(wallet));
        when(walletRepo.save(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        Wallet result = walletService.getOrCreateWallet(10L);
        assertEquals(wallet, result);
    }

    @Test
    void getOrCreateWallet_GenericException_ShouldFallbackToFind() {
        when(walletRepo.findByUserId(10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(wallet));
        when(walletRepo.save(any(Wallet.class)))
                .thenThrow(new RuntimeException("Generic error"));

        Wallet result = walletService.getOrCreateWallet(10L);
        assertEquals(wallet, result);
    }

    @Test
    void getOrCreateWallet_GenericExceptionAndMissing_ShouldThrow() {
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.empty());
        when(walletRepo.save(any(Wallet.class))).thenThrow(new RuntimeException("Generic error"));
        assertThrows(IllegalStateException.class, () -> walletService.getOrCreateWallet(10L));
    }

    @Test
    void addMoney_NullRequest_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> walletService.addMoney(null));
    }

    @Test
    void addMoney_InvalidAmount_ShouldThrow() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(0);
        assertThrows(IllegalArgumentException.class, () -> walletService.addMoney(req));
    }

    @Test
    void addMoney_Valid_ShouldCreditAndPublishEvent() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(200.0);
        req.setDescription("Test top-up");

        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepo.lockById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        Wallet result = walletService.addMoney(req);
        assertEquals(700.0, result.getBalance());
        verify(walletEventPublisher).publishWalletEvent(10L, 200.0, "WALLET_TOPUP_SUCCESS", "Wallet topped up successfully.");
    }

    @Test
    void addMoney_WalletLockFails_ShouldThrow() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(100.0);
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepo.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> walletService.addMoney(req));
    }

    @Test
    void pay_NullRequest_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> walletService.pay(null));
    }

    @Test
    void pay_InsufficientBalance_ShouldThrow() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(1000.0);
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepo.lockById(1L)).thenReturn(Optional.of(wallet));
        assertThrows(ResponseStatusException.class, () -> walletService.pay(req));
    }

    @Test
    void pay_SufficientBalance_ShouldDebitAndPublishEvent() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(200.0);
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepo.lockById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        Wallet result = walletService.pay(req);
        assertEquals(300.0, result.getBalance());
        verify(walletEventPublisher).publishWalletEvent(10L, 200.0, "PAYMENT_SUCCESS", "Payment processed successfully.");
    }

    @Test
    void pay_WalletLockFails_ShouldThrow() {
        WalletRequest req = new WalletRequest();
        req.setUserId(10L);
        req.setAmount(100.0);
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(walletRepo.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> walletService.pay(req));
    }

    @Test
    void getStatements_Valid_ShouldReturnList() {
        when(walletRepo.findByUserId(10L)).thenReturn(Optional.of(wallet));
        when(statementRepo.findByWalletIdOrderByDateTimeDesc(1L)).thenReturn(Collections.emptyList());
        List<Statement> results = walletService.getStatements(10L);
        assertNotNull(results);
    }

    @Test
    void getStatements_NullUserId_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> walletService.getStatements(null));
    }
}
