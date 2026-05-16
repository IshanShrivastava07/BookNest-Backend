package com.booknest.wallet_service.service;

import java.util.List;

import com.booknest.wallet_service.dto.WalletRequest;
import com.booknest.wallet_service.entity.Statement;
import com.booknest.wallet_service.entity.Wallet;

public interface WalletService {
    Wallet createWallet(Long userId);
    Wallet getOrCreateWallet(Long userId);

    /**
     * Credits the wallet (and statement). Intended for trusted internal flows only
     * (e.g. after Razorpay top-up verification). Not exposed on {@code WalletController}.
     */
    Wallet addMoney(WalletRequest request);
    Wallet pay(WalletRequest request);
    List<Statement> getStatements(Long userId);
}
