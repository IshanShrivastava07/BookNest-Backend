package com.booknest.wallet_service.messaging;

import com.booknest.wallet_service.config.RabbitConfig;
import com.booknest.wallet_service.dto.WalletEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WalletEventPublisher publisher;

    @Test
    void publishWalletEvent_Success() {
        publisher.publishWalletEvent(1L, 100.0, "TYPE", "msg");
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_WALLET), any(WalletEvent.class));
    }

    @Test
    void publishWalletEvent_Failure_ShouldLog() {
        doThrow(new RuntimeException("Rabbit down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        // Should not throw
        publisher.publishWalletEvent(1L, 100.0, "TYPE", "msg");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
