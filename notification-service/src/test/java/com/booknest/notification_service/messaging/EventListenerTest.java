package com.booknest.notification_service.messaging;

import com.booknest.notification_service.dto.AuthEvent;
import com.booknest.notification_service.dto.OrderEvent;
import com.booknest.notification_service.dto.WalletEvent;
import com.booknest.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.booknest.notification_service.dto.NotificationRequest;

@ExtendWith(MockitoExtension.class)
class EventListenerTest {

    @Mock NotificationService notificationService;

    // ─── OrderEventListener ───────────────────────────────────────────────────

    @Test
    void orderEventListener_ValidEvent_ShouldDispatchNotification() {
        OrderEventListener listener = new OrderEventListener(notificationService);
        OrderEvent event = new OrderEvent();
        event.setUserId(1L);
        event.setEventType("ORDER_PLACED");
        event.setMessage("Your order was placed.");

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertTrue(captor.getValue().getMessage().contains("ORDER_PLACED"));
    }

    @Test
    void orderEventListener_NullEvent_ShouldNotDispatch() {
        OrderEventListener listener = new OrderEventListener(notificationService);

        listener.onOrderEvent(null);

        verifyNoInteractions(notificationService);
    }

    @Test
    void orderEventListener_NullUserId_ShouldNotDispatch() {
        OrderEventListener listener = new OrderEventListener(notificationService);
        OrderEvent event = new OrderEvent();
        event.setUserId(null);
        event.setEventType("ORDER_PLACED");
        event.setMessage("msg");

        listener.onOrderEvent(event);

        verifyNoInteractions(notificationService);
    }

    @Test
    void orderEventListener_NullEventType_ShouldUseDefaultType() {
        OrderEventListener listener = new OrderEventListener(notificationService);
        OrderEvent event = new OrderEvent();
        event.setUserId(2L);
        event.setEventType(null);
        event.setMessage("Some message");

        listener.onOrderEvent(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("ORDER_EVENT"));
    }

    // ─── AuthEventListener ────────────────────────────────────────────────────

    @Test
    void authEventListener_ValidEvent_ShouldDispatch() {
        AuthEventListener authListener = new AuthEventListener(notificationService);
        AuthEvent event = new AuthEvent(3L, "test@test.com", "Test User", "USER_REGISTERED", "Welcome!");

        authListener.onAuthEvent(event);

        verify(notificationService).send(any(NotificationRequest.class));
    }

    @Test
    void authEventListener_NullEvent_ShouldNotDispatch() {
        AuthEventListener authListener = new AuthEventListener(notificationService);

        authListener.onAuthEvent(null);

        verifyNoInteractions(notificationService);
    }

    // ─── WalletEventListener ──────────────────────────────────────────────────

    @Test
    void walletEventListener_ValidEvent_ShouldDispatch() {
        WalletEventListener walletListener = new WalletEventListener(notificationService);
        WalletEvent event = new WalletEvent(4L, 500.0, "WALLET_TOPUP_SUCCESS", "Wallet topped up.");

        walletListener.onWalletEvent(event);

        verify(notificationService).send(any(NotificationRequest.class));
    }

    @Test
    void walletEventListener_NullEvent_ShouldNotDispatch() {
        WalletEventListener walletListener = new WalletEventListener(notificationService);

        walletListener.onWalletEvent(null);

        verifyNoInteractions(notificationService);
    }
}
