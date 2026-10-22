package com.society.jaswanti.repository;

import com.society.jaswanti.entity.ChargeType;
import com.society.jaswanti.entity.MaintenanceBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceBillRepository extends JpaRepository<MaintenanceBill, Long>, JpaSpecificationExecutor<MaintenanceBill> {
    List<MaintenanceBill> findByRoomId(Long roomId);
    List<MaintenanceBill> findByBillingMonth(String billingMonth);
    List<MaintenanceBill> findByRoomIdAndBillingMonthAndChargeType(Long roomId, String billingMonth, ChargeType chargeType);
    Optional<MaintenanceBill> findFirstByRoomIdAndBillingMonthAndChargeType(Long roomId, String billingMonth, ChargeType chargeType);
    boolean existsByRoomIdAndBillingMonthAndChargeType(Long roomId, String billingMonth, ChargeType chargeType);
    boolean existsByRoomIdAndBillingMonthInAndChargeType(Long roomId, Collection<String> billingMonths, ChargeType chargeType);
    List<MaintenanceBill> findByRoomIdAndBillingMonthInAndChargeType(Long roomId, Collection<String> billingMonths, ChargeType chargeType);
}
