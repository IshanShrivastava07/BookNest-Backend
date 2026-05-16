package com.booknest.notification_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.entity.Notification;
import com.booknest.notification_service.repository.NotificationRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository repo;

    @Override
    public Notification send(NotificationRequest request) {
        Notification n = new Notification();
        n.setUserId(request.getUserId());
        n.setMessage(request.getMessage());
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return repo.save(n);
    }

    @Override
    public List<Notification> getByUser(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not allowed");
        }
        n.setRead(true);
        repo.save(n);
    }
}
