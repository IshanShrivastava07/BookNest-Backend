package com.booknest.wallet_service.controller;

import com.booknest.wallet_service.entity.Wallet;
import com.booknest.wallet_service.service.WalletService;
import com.booknest.wallet_service.service.WalletTopUpService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletTopUpService topUpService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(walletController, "request", request);
        mockMvc = MockMvcBuilders.standaloneSetup(walletController).build();
    }

    @Test
    void getWallet() throws Exception {
        Wallet wallet = new Wallet();
        wallet.setBalance(100.0);
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(walletService.getOrCreateWallet(1L)).thenReturn(wallet);

        mockMvc.perform(get("/wallet")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void createWallet() throws Exception {
        Wallet wallet = new Wallet();
        wallet.setBalance(0.0);
        when(walletService.createWallet(1L)).thenReturn(wallet);

        mockMvc.perform(post("/wallet/create/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void getWallet_MissingHeader_ShouldReturnUnauthorized() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        mockMvc.perform(get("/wallet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWallet_InvalidHeader_ShouldReturnBadRequest() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("abc");
        assertThrows(IllegalArgumentException.class, () -> walletController.getWallet());
    }

    @Test
    void pay_NullBody_ShouldWork() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(walletService.pay(any())).thenReturn(new Wallet());

        mockMvc.perform(post("/wallet/pay")
                .header("X-User-Id", "1")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void statementsLegacy_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/wallet/statement/1"))
                .andExpect(status().isOk());
    }

    @Test
    void statements_ShouldReturnList() throws Exception {
        when(request.getHeader("X-User-Id")).thenReturn("1");
        mockMvc.perform(get("/wallet/statements")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }
}
