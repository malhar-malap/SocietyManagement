package com.society.jaswanti.controller;

import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.ComplaintStatus;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.AnnouncementRepository;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.TransactionRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/dashboard/admin")
public class AdminDashboardController {

    static final int TREND_MONTHS = 6;

    private final UserRepository userRepository;
    private final MaintenanceBillRepository billRepository;
    private final ComplaintRepository complaintRepository;
    private final AnnouncementRepository announcementRepository;
    private final TransactionRepository transactionRepository;
    private final com.society.jaswanti.repository.SocietyBankAccountRepository bankAccountRepository;

    public AdminDashboardController(UserRepository userRepository, MaintenanceBillRepository billRepository,
                                    ComplaintRepository complaintRepository, AnnouncementRepository announcementRepository,
                                    TransactionRepository transactionRepository,
                                    com.society.jaswanti.repository.SocietyBankAccountRepository bankAccountRepository) {
        this.userRepository = userRepository;
        this.billRepository = billRepository;
        this.complaintRepository = complaintRepository;
        this.announcementRepository = announcementRepository;
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> stats = new HashMap<>();
        YearMonth thisMonth = YearMonth.now();

        // Residents who can actually use the portal (not admins, not deactivated or never-onboarded accounts)
        long activeResidents = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.RESIDENT && u.isActive() && u.getStatus() == UserStatus.ACTIVE)
                .count();
        stats.put("totalResidents", activeResidents);

        // Money actually received: successful credit transactions (reversed ones and reversal entries excluded)
        Map<YearMonth, BigDecimal> collectedByMonth = new HashMap<>();
        for (Transaction tx : transactionRepository.findAll()) {
            if (!"CREDIT".equalsIgnoreCase(tx.getEntryDirection()) || !"SUCCESS".equalsIgnoreCase(tx.getStatus()) || tx.getAmount() == null) {
                continue;
            }
            LocalDate date = tx.getPaymentDate() != null ? tx.getPaymentDate()
                    : (tx.getTimestamp() != null ? tx.getTimestamp().toLocalDate() : null);
            if (date != null) {
                collectedByMonth.merge(YearMonth.from(date), tx.getAmount(), BigDecimal::add);
            }
        }
        BigDecimal collectedThisMonth = money(collectedByMonth.getOrDefault(thisMonth, BigDecimal.ZERO));
        BigDecimal collectedLastMonth = money(collectedByMonth.getOrDefault(thisMonth.minusMonths(1), BigDecimal.ZERO));
        stats.put("monthlyCollection", collectedThisMonth);
        stats.put("lastMonthCollection", collectedLastMonth);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.toString());
            point.put("name", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            point.put("amount", money(collectedByMonth.getOrDefault(ym, BigDecimal.ZERO)));
            trend.add(point);
        }
        stats.put("collectionTrend", trend);

        // Outstanding = what is still owed on bills that are not settled (partial payments and late fees included)
        List<MaintenanceBill> bills = billRepository.findAll();
        List<MaintenanceBill> outstanding = bills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.PARTIALLY_PAID)
                .filter(b -> b.getBalance().signum() > 0)
                .toList();
        stats.put("pendingAmount", money(outstanding.stream().map(MaintenanceBill::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add)));
        stats.put("unpaidResidents", outstanding.stream()
                .map(b -> b.getRoom() != null ? b.getRoom().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count());

        // This month's bills by payment status, for the status chart
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        statusBreakdown.put("PAID", 0L);
        statusBreakdown.put("PARTIALLY_PAID", 0L);
        statusBreakdown.put("PENDING", 0L);
        bills.stream()
                .filter(b -> thisMonth.toString().equals(b.getBillingMonth()))
                .forEach(b -> {
                    switch (b.getStatus()) {
                        case PAID, PAID_CASH -> statusBreakdown.merge("PAID", 1L, Long::sum);
                        case PARTIALLY_PAID -> statusBreakdown.merge("PARTIALLY_PAID", 1L, Long::sum);
                        case PENDING -> statusBreakdown.merge("PENDING", 1L, Long::sum);
                        default -> { /* reversed bills are not counted */ }
                    }
                });
        stats.put("billStatusThisMonth", statusBreakdown);
        stats.put("billingMonth", thisMonth.toString());

        long openComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        stats.put("openComplaints", openComplaints);

        LocalDateTime now = LocalDateTime.now();
        long activeAnnouncements = announcementRepository.findAll().stream()
                .filter(a -> a.getExpiryDate() == null || a.getExpiryDate().isAfter(now))
                .count();
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

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
