package com.society.jaswanti.repository;

import com.society.jaswanti.entity.ResidentPaymentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidentPaymentSubmissionRepository extends JpaRepository<ResidentPaymentSubmission, Long> {
    List<ResidentPaymentSubmission> findBySubmittedByUserIdOrderByCreatedAtDesc(Long userId);
    List<ResidentPaymentSubmission> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<ResidentPaymentSubmission> findByStatusOrderByCreatedAtDesc(String status);
    List<ResidentPaymentSubmission> findAllByOrderByCreatedAtDesc();
}
