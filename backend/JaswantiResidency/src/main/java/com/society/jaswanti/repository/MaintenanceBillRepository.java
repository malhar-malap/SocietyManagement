package com.society.jaswanti.repository;

import com.society.jaswanti.entity.MaintenanceBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceBillRepository extends JpaRepository<MaintenanceBill, Long>, JpaSpecificationExecutor<MaintenanceBill> {
    List<MaintenanceBill> findByRoomId(Long roomId);
    List<MaintenanceBill> findByBillingMonth(String billingMonth);
}
