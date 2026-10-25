package com.society.jaswanti.controller;

import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.SystemNotificationRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.SystemNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notifications are side effects of business actions. These tests pin the transaction rules: a failing notification
 * must never undo the action, and a rolled-back action must never send a notification.
 */
@SpringBootTest
class NotificationTransactionSafetyTests {

    @Autowired private SystemNotificationService notificationService;
    @Autowired private SystemNotificationRepository notificationRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String run = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanUp() {
        notificationRepository.findAll().stream()
                .filter(n -> n.getTitle() != null && n.getTitle().endsWith(run))
                .forEach(notificationRepository::delete);
        roomRepository.findByRoomNumber("TX-" + run).ifPresent(roomRepository::delete);
        userRepository.findByEmailIgnoreCase("tx-" + run + "@example.test").ifPresent(userRepository::delete);
    }

    private User unsavedUser() {
        User u = new User();
        u.setEmail("never-saved-" + run + "@example.test");
        u.setName("Never Saved");
        u.setPassword("x");
        u.setRole(Role.RESIDENT);
        u.setStatus(UserStatus.ACTIVE);
        return u; // transient: saving a notification that references it fails at flush time
    }

    @Test
    @DisplayName("A notification that fails in the database does not roll back the business action that triggered it")
    void failingNotificationDoesNotRollBackTheAction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Room room = new Room();
            room.setRoomNumber("TX-" + run);
            room.setWing("T");
            room.setFloor(1);
            roomRepository.save(room);                                            // the business write
            notificationService.createNotification(unsavedUser(), "Broken " + run, "m", null); // fails when written
        });

        assertThat(roomRepository.findByRoomNumber("TX-" + run)).isPresent();
        assertThat(notificationRepository.findAll()).noneMatch(n -> ("Broken " + run).equals(n.getTitle()));
    }

    @Test
    @DisplayName("No notification is sent for an action whose transaction rolls back")
    void rolledBackActionSendsNoNotification() {
        User recipient = new User();
        recipient.setEmail("tx-" + run + "@example.test");
        recipient.setName("Recipient");
        recipient.setPassword("x");
        recipient.setRole(Role.RESIDENT);
        recipient.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(recipient);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.createNotification(saved, "Should not exist " + run, "m", null);
            status.setRollbackOnly(); // e.g. the approval failed after this point
        });

        assertThat(notificationRepository.findAll()).noneMatch(n -> ("Should not exist " + run).equals(n.getTitle()));
    }

    @Test
    @DisplayName("Outside a transaction a notification is written immediately")
    void writesImmediatelyWithoutTransaction() {
        notificationService.createBroadcast("Immediate " + run, "m", null);
        assertThat(notificationRepository.findAll()).anyMatch(n -> ("Immediate " + run).equals(n.getTitle()));
    }
}
