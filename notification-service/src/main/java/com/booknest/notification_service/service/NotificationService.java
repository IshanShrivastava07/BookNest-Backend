package com.booknest.notification_service.service;

import java.util.List;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.entity.Notification;

public interface NotificationService {
    Notification send(NotificationRequest request);
    List<Notification> getByUser(Long userId);
    void markAsRead(Long userId, Long notificationId);
}
