package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditExempt;

import com.society.jaswanti.dto.SystemNotificationDto;
import com.society.jaswanti.service.SystemNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class SystemNotificationController {

    private final SystemNotificationService notificationService;

    public SystemNotificationController(SystemNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    public ResponseEntity<List<SystemNotificationDto>> getMyNotifications(Authentication auth) {
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        // Broadcasts are announcements, which office assistants do not handle
        boolean includeBroadcasts = user.getRole() != com.society.jaswanti.entity.Role.OFFICE_ASSISTANT;
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId(), includeBroadcasts));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    @AuditExempt(reason = "Marking one's own notification as read is a personal UI action with no effect on society records")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
