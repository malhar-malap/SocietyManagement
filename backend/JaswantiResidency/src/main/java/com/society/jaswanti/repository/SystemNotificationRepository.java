package com.society.jaswanti.repository;

import com.society.jaswanti.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SystemNotification> findByUserIdIsNullOrderByCreatedAtDesc();
}
