package com.society.jaswanti.service;

import com.society.jaswanti.dto.SystemNotificationDto;
import com.society.jaswanti.entity.SystemNotification;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.SystemNotificationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemNotificationService {

    private final SystemNotificationRepository notificationRepository;

    public SystemNotificationService(SystemNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createNotification(User user, String title, String message, String link) {
        SystemNotification notification = new SystemNotification(user, title, message, link);
        notificationRepository.save(notification);
    }

    public void createBroadcast(String title, String message, String link) {
        SystemNotification notification = new SystemNotification(null, title, message, link);
        notificationRepository.save(notification);
    }

    public List<SystemNotificationDto> getNotificationsForUser(Long userId) {
        List<SystemNotification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<SystemNotification> broadcastNotifications = notificationRepository.findByUserIdIsNullOrderByCreatedAtDesc();
        
        List<SystemNotification> all = new ArrayList<>();
        all.addAll(userNotifications);
        all.addAll(broadcastNotifications);
        
        // Sort by date descending
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return all.stream()
                .map(n -> {
                    SystemNotificationDto dto = new SystemNotificationDto(
                            n.getId(), n.getTitle(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.getUser() != null && n.isRead());
                    dto.setBroadcast(n.getUser() == null);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Marks one of the user's own notifications as read. Anyone else's notification is refused. A broadcast has a
     * single shared row, so flagging it would mark it read for every user; it is left unchanged and the client
     * remembers per-user read state for broadcasts.
     */
    public void markAsRead(Long id, Long userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser() == null) {
                return;
            }
            if (!n.getUser().getId().equals(userId)) {
                throw new org.springframework.security.access.AccessDeniedException("You can only mark your own notifications as read");
            }
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
