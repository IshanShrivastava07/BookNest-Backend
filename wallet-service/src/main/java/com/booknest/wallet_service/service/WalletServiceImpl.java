package com.booknest.wallet_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.booknest.wallet_service.dto.WalletRequest;
import com.booknest.wallet_service.entity.Statement;
import com.booknest.wallet_service.entity.Wallet;
import com.booknest.wallet_service.messaging.WalletEventPublisher;
import com.booknest.wallet_service.repository.StatementRepository;
import com.booknest.wallet_service.repository.WalletRepository;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final String CREDIT = "CREDIT";
    private static final String DEBIT = "DEBIT";

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private StatementRepository statementRepo;

    @Autowired
    private WalletEventPublisher walletEventPublisher;

    @Override
    public Wallet createWallet(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(0);
        Wallet saved = walletRepo.save(wallet);
        log.info("Wallet created for userId={}", userId);
        return saved;
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepo.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return createWallet(userId);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Concurrent wallet creation for userId={}", userId);
                        return walletRepo.findByUserId(userId)
                                .orElseThrow(() -> new IllegalStateException("Wallet creation failed"));
                    } catch (Exception e) {
                        return walletRepo.findByUserId(userId)
                                .orElseThrow(() -> new IllegalStateException("Wallet creation failed"));
                    }
                });
    }

    @Override
    @Transactional
    public Wallet addMoney(WalletRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        Wallet base = getOrCreateWallet(request.getUserId());
        Wallet wallet = walletRepo.lockById(base.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        walletRepo.save(wallet);
        log.info("Wallet credited for userId={}, amount={}", request.getUserId(), request.getAmount());

        Statement s = new Statement();
        s.setWalletId(wallet.getWalletId());
        s.setUserId(wallet.getUserId());
        s.setType(CREDIT);
        s.setAmount(request.getAmount());
        s.setDateTime(LocalDateTime.now());
        String desc = request.getDescription();
        s.setDescription(desc != null && !desc.isBlank() ? desc.trim() : "Money added");
        statementRepo.save(s);

        walletEventPublisher.publishWalletEvent(wallet.getUserId(), request.getAmount(), "WALLET_TOPUP_SUCCESS", "Wallet topped up successfully.");

        return wallet;
    }

    @Override
    @Transactional
    public Wallet pay(WalletRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        Wallet base = getOrCreateWallet(request.getUserId());
        Wallet wallet = walletRepo.lockById(base.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        if (wallet.getBalance() < request.getAmount()) {
            log.warn("Insufficient balance for userId={}, required={}, available={}", request.getUserId(), request.getAmount(), wallet.getBalance());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - request.getAmount());
        walletRepo.save(wallet);
        log.info("Wallet debited for userId={}, amount={}", request.getUserId(), request.getAmount());

        Statement s = new Statement();
        s.setWalletId(wallet.getWalletId());
        s.setUserId(wallet.getUserId());
        s.setType(DEBIT);
        s.setAmount(request.getAmount());
        s.setDateTime(LocalDateTime.now());
        s.setDescription("Order payment");
        statementRepo.save(s);

        walletEventPublisher.publishWalletEvent(wallet.getUserId(), request.getAmount(), "PAYMENT_SUCCESS", "Payment processed successfully.");

        return wallet;
    }

    @Override
    public List<Statement> getStatements(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        Wallet wallet = getOrCreateWallet(userId);
        return statementRepo.findByWalletIdOrderByDateTimeDesc(wallet.getWalletId());
    }
}
