package com.booknest.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.booknest.order_service.dto.Wallet;
import com.booknest.order_service.dto.WalletRequest;

import feign.codec.ErrorDecoder;

@FeignClient(name = "wallet-service", configuration = WalletClient.FeignConfig.class)
public interface WalletClient {

    @PostMapping("/wallet/pay")
    Wallet pay(@RequestBody WalletRequest request);

    class FeignConfig {
        @Bean
        ErrorDecoder walletErrorDecoder() {
            return new WalletFeignErrorDecoder();
        }
    }
}