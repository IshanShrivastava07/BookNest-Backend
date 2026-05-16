package com.booknest.notification_service.service;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.entity.Notification;
import com.booknest.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository repo;

    @InjectMocks
    private NotificationServiceImpl service;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setUserId(1L);
        notification.setMessage("test");
        notification.setRead(false);
    }

    @Test
    void send_Success() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(1L);
        req.setMessage("test");
        when(repo.save(any(Notification.class))).thenReturn(notification);

        Notification result = service.send(req);
        assertNotNull(result);
        verify(repo).save(any(Notification.class));
    }

    @Test
    void getByUser_Success() {
        when(repo.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertNotNull(service.getByUser(1L));
    }

    @Test
    void markAsRead_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(notification));
        service.markAsRead(1L, 1L);
        assertTrue(notification.isRead());
        verify(repo).save(notification);
    }

    @Test
    void markAsRead_NotFound_ThrowsException() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(1L, 1L));
    }

    @Test
    void markAsRead_NotOwner_ThrowsException() {
        notification.setUserId(2L);
        when(repo.findById(1L)).thenReturn(Optional.of(notification));
        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(1L, 1L));
    }
}
