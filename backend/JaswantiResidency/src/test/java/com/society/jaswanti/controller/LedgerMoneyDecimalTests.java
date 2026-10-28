package com.society.jaswanti.controller;

import com.society.jaswanti.dto.MaintenanceBillDto;
import com.society.jaswanti.dto.TransactionDto;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.TransactionRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.MaintenanceBillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LedgerMoneyDecimalTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.society.jaswanti.support.TestDatabase testDatabase;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceBillService billService;

    @Autowired
    private AdminDashboardController adminDashboardController;

    private Room testRoom;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        transactionRepository.deleteAll();
        billRepository.deleteAll();

        userRepository.findByEmail("admin_ledger_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Ledger");
            u.setEmail("admin_ledger_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        testRoom = roomRepository.findAll().stream().findFirst().orElseGet(() -> {
            Room r = new Room();
            r.setRoomNumber("T-999");
            r.setFloor(9);
            r.setWing("T");
            return roomRepository.save(r);
        });
    }

    @Test
    @DisplayName("Exact decimal storage and retrieval in database/JPA for MaintenanceBill and Transaction")
    void testExactDecimalStorageAndRetrieval() {
        BigDecimal billAmount = new BigDecimal("3500.50");
        BigDecimal lateFee = new BigDecimal("150.25");

        MaintenanceBill bill = new MaintenanceBill(testRoom, "November 2026", billAmount, LocalDate.now().plusDays(15));
        bill.setLateFee(lateFee);
        MaintenanceBill savedBill = billRepository.save(bill);

        MaintenanceBill retrievedBill = billRepository.findById(savedBill.getId()).orElseThrow();
        assertThat(retrievedBill.getAmount()).isEqualByComparingTo(billAmount);
        assertThat(retrievedBill.getAmount().scale()).isEqualTo(2);
        assertThat(retrievedBill.getLateFee()).isEqualByComparingTo(lateFee);
        assertThat(retrievedBill.getLateFee().scale()).isEqualTo(2);

        BigDecimal txAmount = new BigDecimal("3650.75");
        Transaction tx = new Transaction(retrievedBill, txAmount, "SUCCESS", "CASH");
        Transaction savedTx = transactionRepository.save(tx);

        Transaction retrievedTx = transactionRepository.findById(savedTx.getId()).orElseThrow();
        assertThat(retrievedTx.getAmount()).isEqualByComparingTo(txAmount);
        assertThat(retrievedTx.getAmount().scale()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "admin_ledger_test@jaswanti.com", roles = "ADMIN")
    @DisplayName("Arithmetic precision avoids IEEE 754 floating-point drift (e.g. 0.10 + 0.20 == 0.30)")
    void testAdditionSubtractionWithoutFloatingPointDrift() {
        // In IEEE 754 float/double, 0.1 + 0.2 produces 0.30000000000000004
        double floatSum = 0.1 + 0.2;
        assertThat(floatSum).isNotEqualTo(0.3);

        // With BigDecimal scale 2, 0.10 + 0.20 produces exactly 0.30
        BigDecimal d1 = new BigDecimal("0.10");
        BigDecimal d2 = new BigDecimal("0.20");
        BigDecimal exactSum = d1.add(d2).setScale(2, RoundingMode.HALF_UP);
        assertThat(exactSum).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(exactSum.toPlainString()).isEqualTo("0.30");

        // Verify markPaidCash computes exact total amount without floating-point artifacts
        MaintenanceBill bill = new MaintenanceBill(testRoom, "December 2026", new BigDecimal("3500.10"), LocalDate.now().plusDays(10));
        bill.setLateFee(new BigDecimal("0.20"));
        MaintenanceBill savedBill = billRepository.save(bill);

        billService.markPaidCash(savedBill.getId());

        List<Transaction> txs = transactionRepository.findByBillId(savedBill.getId());
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("3500.30"));
        assertThat(txs.get(0).getAmount().toPlainString()).isEqualTo("3500.30");
    }

    @Test
    @DisplayName("Scale normalization and HALF_UP rounding at input boundaries")
    void testScaleAndRoundingNormalization() {
        MaintenanceBill bill = new MaintenanceBill();
        
        // Single fractional digit normalizes to scale 2
        bill.setAmount(new BigDecimal("3500.5"));
        assertThat(bill.getAmount().scale()).isEqualTo(2);
        assertThat(bill.getAmount().toPlainString()).isEqualTo("3500.50");

        // Half-up rounding on 3 decimal digits (.555 rounds up to .56)
        bill.setAmount(new BigDecimal("3500.555"));
        assertThat(bill.getAmount().scale()).isEqualTo(2);
        assertThat(bill.getAmount().toPlainString()).isEqualTo("3500.56");

        // Half-up rounding on 3 decimal digits (.554 rounds down to .55)
        bill.setAmount(new BigDecimal("3500.554"));
        assertThat(bill.getAmount().scale()).isEqualTo(2);
        assertThat(bill.getAmount().toPlainString()).isEqualTo("3500.55");

        // Late fee normalizes to scale 2
        bill.setLateFee(new BigDecimal("25.1"));
        assertThat(bill.getLateFee().scale()).isEqualTo(2);
        assertThat(bill.getLateFee().toPlainString()).isEqualTo("25.10");

        // DTO setters also normalize scale 2 with HALF_UP
        MaintenanceBillDto dto = new MaintenanceBillDto();
        dto.setAmount(new BigDecimal("1234.567"));
        assertThat(dto.getAmount().scale()).isEqualTo(2);
        assertThat(dto.getAmount().toPlainString()).isEqualTo("1234.57");

        TransactionDto txDto = new TransactionDto();
        txDto.setAmount(new BigDecimal("99.9"));
        assertThat(txDto.getAmount().scale()).isEqualTo(2);
        assertThat(txDto.getAmount().toPlainString()).isEqualTo("99.90");
    }

    @Test
    @DisplayName("Backwards compatibility with Double constructor and setters")
    void testBackwardsCompatibilityWithDouble() {
        // Constructor accepting Double converts to normalized BigDecimal
        MaintenanceBill bill = new MaintenanceBill(testRoom, "January 2027", 4200.75, LocalDate.now().plusDays(20));
        assertThat(bill.getAmount()).isEqualByComparingTo(new BigDecimal("4200.75"));
        assertThat(bill.getAmount().scale()).isEqualTo(2);

        // Setter accepting Double converts to normalized BigDecimal
        bill.setAmount(1800.5);
        assertThat(bill.getAmount()).isEqualByComparingTo(new BigDecimal("1800.50"));
        assertThat(bill.getAmount().scale()).isEqualTo(2);

        bill.setLateFee(75.255);
        assertThat(bill.getLateFee()).isEqualByComparingTo(new BigDecimal("75.26"));
        assertThat(bill.getLateFee().scale()).isEqualTo(2);

        // Transaction constructor with Double
        Transaction tx = new Transaction(bill, 1875.76, "SUCCESS", "CASH");
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("1875.76"));
        assertThat(tx.getAmount().scale()).isEqualTo(2);

        // DTO constructors with Double
        MaintenanceBillDto billDto = new MaintenanceBillDto(1L, testRoom.getId(), "T-999", "Test Resident",
                "January 2027", 3500.0, LocalDate.now(), LocalDate.now().minusDays(15), 0.0, BillStatus.PENDING);
        assertThat(billDto.getAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(billDto.getLateFee()).isEqualByComparingTo(new BigDecimal("0.00"));

        TransactionDto txDto = new TransactionDto(1L, 1L, "Test Resident", "T-999", "CASH_123",
                "CASH", 3500.0, "SUCCESS", java.time.LocalDateTime.now(), "January 2027");
        assertThat(txDto.getAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));

        // MaintenanceBillService Double overload
        List<MaintenanceBillDto> generated = billService.generateMonthlyBills("2027-02", 3500.0, LocalDate.now().plusDays(10));
        assertThat(generated).isNotEmpty();
        assertThat(generated.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin dashboard calculates exact collection and pending sums using BigDecimal")
    void testAdminDashboardPrecision() {
        // "Collected this month" is money actually received: successful credit transactions dated this month
        for (String amount : new String[] {"1000.10", "2000.20"}) {
            Transaction received = new Transaction();
            received.setAmount(new BigDecimal(amount));
            received.setEntryDirection("CREDIT");
            received.setStatus("SUCCESS");
            received.setPaymentMode("CASH");
            received.setPaymentDate(LocalDate.now());
            transactionRepository.save(received);
        }
        MaintenanceBill paid = new MaintenanceBill(testRoom, "Paid-Month-1", new BigDecimal("3000.30"), LocalDate.now());
        paid.setPaidAmount(new BigDecimal("3000.30"));
        paid.setStatus(BillStatus.PAID);
        billRepository.save(paid);

        MaintenanceBill pending = new MaintenanceBill(testRoom, "Pending-Month", new BigDecimal("500.10"), LocalDate.now());
        pending.setLateFee(new BigDecimal("0.20"));
        pending.setStatus(BillStatus.PENDING);
        billRepository.save(pending);

        ResponseEntity<Map<String, Object>> response = adminDashboardController.getAdminDashboard(null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> stats = response.getBody();
        assertThat(stats).isNotNull();

        Object monthlyCollection = stats.get("monthlyCollection");
        assertThat(monthlyCollection).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) monthlyCollection).isEqualByComparingTo(new BigDecimal("3000.30"));

        Object pendingAmount = stats.get("pendingAmount");
        assertThat(pendingAmount).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) pendingAmount).isEqualByComparingTo(new BigDecimal("500.30"));
    }
}
