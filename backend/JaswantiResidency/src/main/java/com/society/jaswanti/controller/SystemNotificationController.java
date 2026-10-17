package com.society.jaswanti.controller;

import com.society.jaswanti.dto.SystemNotificationDto;
import com.society.jaswanti.service.SystemNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class SystemNotificationController {

    private final SystemNotificationService notificationService;

    public SystemNotificationController(SystemNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<SystemNotificationDto>> getMyNotifications(Authentication auth) {
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
