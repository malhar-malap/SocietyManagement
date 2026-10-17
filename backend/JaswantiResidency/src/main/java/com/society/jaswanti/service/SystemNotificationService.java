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
                .map(n -> new SystemNotificationDto(
                        n.getId(), n.getTitle(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.isRead()))
                .collect(Collectors.toList());
    }
    
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
