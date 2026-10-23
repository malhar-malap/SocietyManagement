package com.society.jaswanti.repository;

import com.society.jaswanti.entity.TransactionAmendmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAmendmentRequestRepository extends JpaRepository<TransactionAmendmentRequest, Long> {
    List<TransactionAmendmentRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<TransactionAmendmentRequest> findAllByOrderByCreatedAtDesc();
}
