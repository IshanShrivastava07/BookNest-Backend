package com.booknest.auth_service.messaging;

import com.booknest.auth_service.config.RabbitConfig;
import com.booknest.auth_service.dto.AuthEvent;
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
class AuthEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthEventPublisher authEventPublisher;

    @Test
    void publishUserRegistered_Success_ShouldCallConvertAndSend() {
        authEventPublisher.publishUserRegistered(1L, "test@example.com", "Test User");

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY_AUTH), any(AuthEvent.class));
    }

    @Test
    void publishUserRegistered_Failure_ShouldSwallowException() {
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Should not throw exception
        authEventPublisher.publishUserRegistered(1L, "test@example.com", "Test User");

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
