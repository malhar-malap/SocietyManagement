package com.society.jaswanti.repository;

import com.society.jaswanti.entity.SocietyBankAccount;
import com.society.jaswanti.entity.SocietyBankAccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocietyBankAccountRepository extends JpaRepository<SocietyBankAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM SocietyBankAccount b WHERE b.id = :id")
    Optional<SocietyBankAccount> findByIdForUpdate(@Param("id") Long id);

    List<SocietyBankAccount> findByIsActiveTrue();

    Optional<SocietyBankAccount> findFirstByAccountTypeAndIsActiveTrue(SocietyBankAccountType accountType);

    Optional<SocietyBankAccount> findFirstByIsActiveTrue();
}
