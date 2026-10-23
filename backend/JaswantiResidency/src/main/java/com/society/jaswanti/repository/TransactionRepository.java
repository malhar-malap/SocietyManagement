package com.society.jaswanti.repository;

import com.society.jaswanti.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBillId(Long billId);
    List<Transaction> findByBankAccountId(Long bankAccountId);
    Optional<Transaction> findByGatewayPaymentId(String gatewayPaymentId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findByPaymentModeAndReferenceNumber(String paymentMode, String referenceNumber);
    boolean existsByReceiptNumber(String receiptNumber);
}
