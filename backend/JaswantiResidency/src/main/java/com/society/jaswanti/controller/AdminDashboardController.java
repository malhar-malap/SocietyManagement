package com.society.jaswanti.controller;

import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.ComplaintStatus;
import com.society.jaswanti.repository.AnnouncementRepository;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final MaintenanceBillRepository billRepository;
    private final ComplaintRepository complaintRepository;
    private final AnnouncementRepository announcementRepository;
    private final com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;
    private final com.society.jaswanti.repository.SocietyBankAccountRepository bankAccountRepository;

    public AdminDashboardController(UserRepository userRepository, MaintenanceBillRepository billRepository,
                                    ComplaintRepository complaintRepository, AnnouncementRepository announcementRepository,
                                    com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository,
                                    com.society.jaswanti.repository.SocietyBankAccountRepository bankAccountRepository) {
        this.userRepository = userRepository;
        this.billRepository = billRepository;
        this.complaintRepository = complaintRepository;
        this.announcementRepository = announcementRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalResidents = userRepository.count();
        stats.put("totalResidents", totalResidents);
        
        BigDecimal monthlyCollection = billRepository.findAll().stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .map(b -> b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        stats.put("monthlyCollection", monthlyCollection);
        
        BigDecimal pendingAmount = billRepository.findAll().stream()
                .filter(b -> b.getStatus() != BillStatus.PAID)
                .map(b -> {
                    BigDecimal amt = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
                    BigDecimal fee = b.getLateFee() != null ? b.getLateFee() : BigDecimal.ZERO;
                    return amt.add(fee);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        stats.put("pendingAmount", pendingAmount);
        
        long openComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        stats.put("openComplaints", openComplaints);
        
        long unpaidResidents = billRepository.findAll().stream()
                .filter(b -> b.getStatus() != BillStatus.PAID)
                .map(b -> resolveResidentId(b.getRoom()))
                .filter(id -> id != -1L)
                .distinct()
                .count();
        stats.put("unpaidResidents", unpaidResidents);

        long activeAnnouncements = announcementRepository.findAll().size(); // Or filter active ones
        stats.put("activeAnnouncements", activeAnnouncements);

        List<com.society.jaswanti.entity.SocietyBankAccount> accounts = bankAccountRepository.findByIsActiveTrue();
        stats.put("bankAccounts", accounts);

        BigDecimal totalBankBalance = accounts.stream()
                .filter(a -> a.getAccountType() == com.society.jaswanti.entity.SocietyBankAccountType.BANK)
                .map(com.society.jaswanti.entity.SocietyBankAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        stats.put("totalBankBalance", totalBankBalance);

        BigDecimal totalCashBalance = accounts.stream()
                .filter(a -> a.getAccountType() == com.society.jaswanti.entity.SocietyBankAccountType.CASH_LEDGER)
                .map(com.society.jaswanti.entity.SocietyBankAccount::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        stats.put("totalCashBalance", totalCashBalance);

        stats.put("totalLiquidBalance", totalBankBalance.add(totalCashBalance).setScale(2, RoundingMode.HALF_UP));

        return ResponseEntity.ok(stats);
    }

    private Long resolveResidentId(com.society.jaswanti.entity.Room room) {
        if (room == null) return -1L;
        return roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(room.getId())
                .map(rr -> rr.getResident() != null ? rr.getResident().getId() : -1L)
                .orElseGet(() -> {
                    if (!roomResidentRepository.existsByRoomId(room.getId())) {
                        return room.getResident() != null ? room.getResident().getId() : -1L;
                    }
                    return -1L;
                });
    }
}
