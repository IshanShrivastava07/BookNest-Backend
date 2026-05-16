package com.booknest.notification_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.booknest.notification_service.dto.NotificationRequest;
import com.booknest.notification_service.entity.Notification;
import com.booknest.notification_service.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private HttpServletRequest request;

    private Long getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("SOURCE: NOTIFICATION-SERVICE | Missing X-User-Id header");
        }
        return Long.parseLong(userIdStr);
    }

    @PostMapping
    public Notification send(@RequestBody NotificationRequest body) {
        return service.send(body);
    }

    @GetMapping
    public List<Notification> listMine() {
        return service.getByUser(getCurrentUserId());
    }

    @GetMapping("/{userId}")
    public List<Notification> listForUser(@PathVariable Long userId) {
        if (!getCurrentUserId().equals(userId)) {
            throw new IllegalStateException("Not allowed");
        }
        return service.getByUser(userId);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        try {
            service.markAsRead(getCurrentUserId(), id);
            return ResponseEntity.ok("Marked as read");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> markReadLegacy(@PathVariable Long id) {
        return markRead(id);
    }
}
