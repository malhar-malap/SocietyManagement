package com.society.jaswanti.repository;

import com.society.jaswanti.entity.ReceiptSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM ReceiptSequence rs WHERE rs.period = :period")
    Optional<ReceiptSequence> findByPeriodForUpdate(@Param("period") String period);
}
