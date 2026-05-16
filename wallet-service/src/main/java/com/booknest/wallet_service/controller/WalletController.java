package com.booknest.wallet_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.booknest.wallet_service.dto.CreateTopupOrderRequest;
import com.booknest.wallet_service.dto.CreateTopupOrderResponse;
import com.booknest.wallet_service.dto.VerifyTopupRequest;
import com.booknest.wallet_service.dto.VerifyTopupResponse;
import com.booknest.wallet_service.dto.WalletRequest;
import com.booknest.wallet_service.entity.Statement;
import com.booknest.wallet_service.entity.Wallet;
import com.booknest.wallet_service.service.WalletService;
import com.booknest.wallet_service.service.WalletTopUpService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService service;

    @Autowired
    private WalletTopUpService topUpService;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user id");
        }
    }

    @GetMapping
    public Wallet getWallet() {
        return service.getOrCreateWallet(getCurrentUserId());
    }

    @PostMapping("/create/{userId}")
    public Wallet create(@PathVariable Long userId) {
        return service.createWallet(userId);
    }

    @PostMapping("/create-topup-order")
    public CreateTopupOrderResponse createTopupOrder(@RequestBody CreateTopupOrderRequest body) {
        return topUpService.createTopupOrder(getCurrentUserId(), body);
    }

    @PostMapping("/verify-topup")
    public VerifyTopupResponse verifyTopup(@RequestBody VerifyTopupRequest body) {
        return topUpService.verifyTopup(getCurrentUserId(), body);
    }

    @PostMapping("/pay")
    public Wallet pay(@RequestBody WalletRequest body) {
        if (body == null) {
            body = new WalletRequest();
        }
        body.setUserId(getCurrentUserId());
        return service.pay(body);
    }

    @GetMapping("/statement/{userId}")
    public List<Statement> statementsLegacy(@PathVariable Long userId) {
        return service.getStatements(userId);
    }

    @GetMapping("/statements")
    public List<Statement> statements() {
        return service.getStatements(getCurrentUserId());
    }
}
