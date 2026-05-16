package com.booknest.wallet_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.wallet_service.entity.Statement;

public interface StatementRepository extends JpaRepository<Statement, Long> {
    List<Statement> findByWalletId(Long walletId);

    List<Statement> findByWalletIdOrderByDateTimeDesc(Long walletId);
}