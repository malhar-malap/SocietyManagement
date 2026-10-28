package com.society.jaswanti.service;

import com.society.jaswanti.dto.SystemNotificationDto;
import com.society.jaswanti.entity.SystemNotification;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.SystemNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SystemNotificationService.class);

    private final SystemNotificationRepository notificationRepository;
    private final TransactionTemplate separateTransaction;

    public SystemNotificationService(SystemNotificationRepository notificationRepository,
                                     PlatformTransactionManager transactionManager) {
        this.notificationRepository = notificationRepository;
        this.separateTransaction = new TransactionTemplate(transactionManager);
        this.separateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void createNotification(User user, String title, String message, String link) {
        afterCommit(() -> notificationRepository.save(new SystemNotification(user, title, message, link)));
    }

    public void createBroadcast(String title, String message, String link) {
        afterCommit(() -> notificationRepository.save(new SystemNotification(null, title, message, link)));
    }

    /**
     * Notifications are a side effect of a business action (approving a payment, answering a complaint), so they
     * are written only after that action's transaction has committed, in a transaction of their own:
     * - a notification is never sent for an action that rolled back, and
     * - a failing notification can never roll back the action. Saving inside the caller's transaction would mark it
     *   rollback-only on failure even if the caller caught the exception, undoing e.g. an approved payment.
     * Failures are logged and swallowed. Outside a transaction the notification is written immediately.
     */
    private void afterCommit(Runnable write) {
        Runnable safeWrite = () -> {
            try {
                separateTransaction.executeWithoutResult(status -> write.run());
            } catch (RuntimeException e) {
                logger.warn("Could not create notification: {}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeWrite.run();
                }
            });
        } else {
            safeWrite.run();
        }
    }

    public List<SystemNotificationDto> getNotificationsForUser(Long userId) {
        return getNotificationsForUser(userId, true);
    }

    public List<SystemNotificationDto> getNotificationsForUser(Long userId, boolean includeBroadcasts) {
        List<SystemNotification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<SystemNotification> broadcastNotifications = includeBroadcasts
                ? notificationRepository.findByUserIdIsNullOrderByCreatedAtDesc()
                : List.of();
        
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
