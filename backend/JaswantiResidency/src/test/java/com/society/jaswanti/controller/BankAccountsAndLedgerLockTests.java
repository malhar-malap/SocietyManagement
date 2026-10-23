package com.society.jaswanti.controller;

import com.society.jaswanti.dto.RecordPaymentRequest;
import com.society.jaswanti.dto.RecordPaymentResponse;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.PaymentRecordingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankAccountsAndLedgerLockTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocietyBankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionAmendmentRequestRepository amendmentRequestRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PaymentRecordingService paymentRecordingService;

    private User admin;
    private User masterAdmin;
    private User resident;
    private String adminToken;
    private String masterAdminToken;
    private String residentToken;

    private SocietyBankAccount bankAccount;
    private SocietyBankAccount cashAccount;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        paymentAttemptRepository.deleteAll();
        amendmentRequestRepository.deleteAll();
        transactionRepository.deleteAll();
        activityLogRepository.deleteAll();
        billRepository.deleteAll();
        bankAccountRepository.deleteAll();

        admin = userRepository.findByEmail("admin_bank_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Bank Test");
            u.setEmail("admin_bank_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        masterAdmin = userRepository.findByEmail("master_admin_bank_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Master Admin Bank Test");
            u.setEmail("master_admin_bank_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.MASTER_ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        resident = userRepository.findByEmail("resident_bank_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident Bank Test");
            u.setEmail("resident_bank_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        adminToken = jwtService.generateToken(admin);
        masterAdminToken = jwtService.generateToken(masterAdmin);
        residentToken = jwtService.generateToken(resident);

        testRoom = roomRepository.findAll().stream().findFirst().orElseGet(() -> {
            Room r = new Room();
            r.setRoomNumber("B-101");
            r.setFloor(1);
            r.setWing("B");
            return roomRepository.save(r);
        });

        bankAccount = new SocietyBankAccount("Main Maintenance Bank A/c", "HDFC Bank", "50100456789123", "HDFC0001234",
                SocietyBankAccountType.BANK, new BigDecimal("10000.00"));
        bankAccount = bankAccountRepository.save(bankAccount);

        cashAccount = new SocietyBankAccount("Petty Cash Ledger", null, null, null,
                SocietyBankAccountType.CASH_LEDGER, new BigDecimal("2000.00"));
        cashAccount = bankAccountRepository.save(cashAccount);
    }

    @Test
    @DisplayName("GET /api/bank-accounts allowed for Admin & Master Admin; 403 Forbidden for Resident")
    void testBankAccountsReadPermissions() throws Exception {
        mockMvc.perform(get("/api/bank-accounts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/bank-accounts")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bank-accounts")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST & PUT /api/bank-accounts restricted strictly to Master Admin; Admin gets 403 Forbidden")
    void testCreateAndEditBankAccountMasterAdminOnly() throws Exception {
        String createJson = """
                {
                    "accountName": "Sinking Fund A/c",
                    "bankName": "SBI",
                    "accountNumber": "12345678901",
                    "ifscCode": "SBIN0001111",
                    "accountType": "BANK",
                    "openingBalance": 50000.00
                }
                """;

        // Admin cannot create bank account
        mockMvc.perform(post("/api/bank-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isForbidden());

        // Master Admin can create bank account
        mockMvc.perform(post("/api/bank-accounts")
                        .header("Authorization", "Bearer " + masterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Sinking Fund A/c"))
                .andExpect(jsonPath("$.openingBalance").value(50000.00))
                .andExpect(jsonPath("$.currentBalance").value(50000.00));

        SocietyBankAccount created = bankAccountRepository.findAll().stream()
                .filter(a -> "Sinking Fund A/c".equals(a.getAccountName()))
                .findFirst().orElseThrow();

        // Master Admin can update account
        String updateJson = """
                {
                    "accountName": "Sinking Fund High Yield A/c"
                }
                """;
        mockMvc.perform(put("/api/bank-accounts/" + created.getId())
                        .header("Authorization", "Bearer " + masterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("Sinking Fund High Yield A/c"));
    }

    @Test
    @DisplayName("Recording payment (CREDIT) automatically increments SocietyBankAccount current_balance")
    void testPaymentRecordingUpdatesLiveBankAccountBalance() throws Exception {
        BigDecimal initialBal = bankAccount.getCurrentBalance();

        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("3500.00"), LocalDate.now().plusDays(15));
        bill = billRepository.save(bill);

        String payJson = String.format("""
                {
                    "amount": 3500.00,
                    "paymentMode": "BANK_TRANSFER",
                    "referenceNumber": "IMPS-987654",
                    "paymentDate": "%s",
                    "notes": "Bank transfer payment",
                    "bankAccountId": %d
                }
                """, LocalDate.now(), bankAccount.getId());

        mockMvc.perform(post("/api/bills/" + bill.getId() + "/record-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billStatus").value("PAID"));

        SocietyBankAccount updated = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo(initialBal.add(new BigDecimal("3500.00")));
    }

    @Test
    @DisplayName("Recording expense (DEBIT) automatically decrements SocietyBankAccount current_balance")
    void testExpenseRecordingDebitsLiveBankAccountBalance() throws Exception {
        BigDecimal initialBal = cashAccount.getCurrentBalance(); // 2000.00

        String expenseJson = String.format("""
                {
                    "amount": 800.00,
                    "category": "DAILY_WAGES",
                    "bankAccountId": %d,
                    "paymentMode": "CASH",
                    "paymentDate": "%s",
                    "payeeName": "Ramesh Guard",
                    "notes": "Daily security wages"
                }
                """, cashAccount.getId(), LocalDate.now());

        mockMvc.perform(post("/api/transactions/expense")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(800.00))
                .andExpect(jsonPath("$.entryDirection").value("DEBIT"))
                .andExpect(jsonPath("$.category").value("DAILY_WAGES"));

        SocietyBankAccount updated = bankAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo(initialBal.subtract(new BigDecimal("800.00"))); // 1200.00
    }

    @Test
    @DisplayName("Immediate Reversal within 24h: Admin can reverse, restores bank balance, restores bill balance, writes audit")
    void testImmediateReversalWithin24HoursRestoresBalances() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("4000.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        // Record a payment of 4000 into bankAccount
        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("4000.00"), PaymentMode.BANK_TRANSFER, "TXN-REV-TEST", LocalDate.now(), "Initial payment"
        );
        payReq.setBankAccountId(bankAccount.getId());
        RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        SocietyBankAccount afterPayAccount = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(afterPayAccount.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("14000.00")); // 10000 + 4000

        // Reverse within 24 hours
        String revJson = """
                {
                    "reason": "Resident transferred from wrong account"
                }
                """;
        mockMvc.perform(post("/api/transactions/" + payResp.getTransactionId() + "/reverse")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(false))
                .andExpect(jsonPath("$.message").value("Transaction reversed successfully"));

        // Verify bank balance restored to 10000.00
        SocietyBankAccount afterRevAccount = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(afterRevAccount.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));

        // Verify bill restored to PENDING with paid_amount 0.00
        MaintenanceBill afterRevBill = billRepository.findById(bill.getId()).orElseThrow();
        assertThat(afterRevBill.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(afterRevBill.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify original tx marked REVERSED and compensating transaction exists
        Transaction origTx = transactionRepository.findById(payResp.getTransactionId()).orElseThrow();
        assertThat(origTx.getStatus()).isEqualTo("REVERSED");

        assertThat(transactionRepository.findAll().stream().anyMatch(t -> "REVERSAL".equals(t.getStatus()))).isTrue();
    }

    @Test
    @DisplayName("24-Hour Admin Edit Lock: Admin reversal after 24h does NOT mutate balances; returns 202 and queues amendment")
    void testReversalAfter24HoursLockedForAdminAndCreatesAmendmentRequest202() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("3000.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("3000.00"), PaymentMode.BANK_TRANSFER, "TXN-OLD-1", LocalDate.now().minusDays(2), "Old payment"
        );
        payReq.setBankAccountId(bankAccount.getId());
        RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        // Manually age the transaction past 24 hours (e.g. 26 hours ago)
        Transaction tx = transactionRepository.findById(payResp.getTransactionId()).orElseThrow();
        tx.setApprovedAt(LocalDateTime.now().minusHours(26));
        transactionRepository.save(tx);

        BigDecimal balanceBefore = bankAccountRepository.findById(bankAccount.getId()).orElseThrow().getCurrentBalance();

        // Admin attempts reversal after 24 hours
        String revJson = """
                {
                    "reason": "Old mistake discovered after 2 days"
                }
                """;
        mockMvc.perform(post("/api/transactions/" + tx.getId() + "/reverse")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pendingApproval").value(true))
                .andExpect(jsonPath("$.amendmentRequestId").isNumber());

        // Balance must NOT have changed!
        BigDecimal balanceAfter = bankAccountRepository.findById(bankAccount.getId()).orElseThrow().getCurrentBalance();
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore);

        // Transaction must still be SUCCESS (not REVERSED yet)
        Transaction unchangedTx = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(unchangedTx.getStatus()).isEqualTo("SUCCESS");

        // Amendment request exists in PENDING state
        assertThat(amendmentRequestRepository.findAll()).hasSize(1);
        TransactionAmendmentRequest req = amendmentRequestRepository.findAll().get(0);
        assertThat(req.getStatus()).isEqualTo("PENDING");
        assertThat(req.getActionType()).isEqualTo("REVERSE");
    }

    @Test
    @DisplayName("Master Admin direct reversal after 24 hours bypasses the lock and executes immediately (HTTP 200)")
    void testMasterAdminDirectReversalAfter24HoursBypassesLock() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("2500.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("2500.00"), PaymentMode.BANK_TRANSFER, "TXN-MASTER-BYPASS", LocalDate.now().minusDays(5), "Past payment"
        );
        payReq.setBankAccountId(bankAccount.getId());
        RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        // Age transaction 72 hours ago
        Transaction tx = transactionRepository.findById(payResp.getTransactionId()).orElseThrow();
        tx.setApprovedAt(LocalDateTime.now().minusHours(72));
        transactionRepository.save(tx);

        // Master Admin reverses directly
        String revJson = """
                {
                    "reason": "Committee authorized reversal after audit"
                }
                """;
        mockMvc.perform(post("/api/transactions/" + tx.getId() + "/reverse")
                        .header("Authorization", "Bearer " + masterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval").value(false));

        Transaction reversedTx = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reversedTx.getStatus()).isEqualTo("REVERSED");
    }

    @Test
    @DisplayName("Master Admin resolves pending amendment request: Approving executes reversal and updates live balance")
    void testMasterAdminApprovesQueuedAmendmentAppliesReversal() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("5000.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("5000.00"), PaymentMode.BANK_TRANSFER, "TXN-QUEUE-TEST", LocalDate.now().minusDays(2), "Payment"
        );
        payReq.setBankAccountId(bankAccount.getId());
        RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        // Age tx 30 hours
        Transaction tx = transactionRepository.findById(payResp.getTransactionId()).orElseThrow();
        tx.setApprovedAt(LocalDateTime.now().minusHours(30));
        transactionRepository.save(tx);

        // Admin queues reversal
        String revJson = """
                {
                    "reason": "Admin audit catch"
                }
                """;
        mockMvc.perform(post("/api/transactions/" + tx.getId() + "/reverse")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().isAccepted());

        TransactionAmendmentRequest req = amendmentRequestRepository.findAll().get(0);
        assertThat(req.getStatus()).isEqualTo("PENDING");

        // Master Admin views requests
        mockMvc.perform(get("/api/transactions/amendment-requests")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Master Admin approves request
        String resolveJson = """
                {
                    "action": "APPROVE",
                    "reviewNotes": "Approved during committee meeting"
                }
                """;
        mockMvc.perform(post("/api/transactions/amendment-requests/" + req.getId() + "/resolve")
                        .header("Authorization", "Bearer " + masterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Bank balance restored
        SocietyBankAccount acc = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(acc.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));

        // Bill restored
        MaintenanceBill b = billRepository.findById(bill.getId()).orElseThrow();
        assertThat(b.getStatus()).isEqualTo(BillStatus.PENDING);
    }

    @Test
    @DisplayName("Double reversal on already reversed transaction is rejected")
    void testDoubleReversalRejected() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("1000.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("1000.00"), PaymentMode.CASH, null, LocalDate.now(), "Cash pay"
        );
        RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        // Reverse once
        String revJson = """
                {
                    "reason": "First reversal"
                }
                """;
        mockMvc.perform(post("/api/transactions/" + payResp.getTransactionId() + "/reverse")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().isOk());

        // Reverse second time -> error
        mockMvc.perform(post("/api/transactions/" + payResp.getTransactionId() + "/reverse")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revJson))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Bank reconciliation calculates openingBalance + credits - debits and matches current balance")
    void testBankReconciliation() throws Exception {
        // 1. Initial account state
        // bankAccount has opening_balance 10,000, current_balance 10,000
        mockMvc.perform(get("/api/bank-accounts/" + bankAccount.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(10000.00))
                .andExpect(jsonPath("$.totalCredits").value(0.00))
                .andExpect(jsonPath("$.totalDebits").value(0.00))
                .andExpect(jsonPath("$.calculatedBalance").value(10000.00))
                .andExpect(jsonPath("$.currentBalance").value(10000.00))
                .andExpect(jsonPath("$.discrepancy").value(0.00))
                .andExpect(jsonPath("$.reconciled").value(true));

        // 2. Perform a credit payment linked to bankAccount
        MaintenanceBill bill = new MaintenanceBill(testRoom, "2026-10", new BigDecimal("4500.00"), LocalDate.now().plusDays(10));
        bill = billRepository.save(bill);

        RecordPaymentRequest payReq = new RecordPaymentRequest(
                new BigDecimal("4500.00"), PaymentMode.BANK_TRANSFER, "NEFT-123", LocalDate.now(), "Bank transfer payment"
        );
        payReq.setBankAccountId(bankAccount.getId());
        paymentRecordingService.recordPayment(bill.getId(), null, payReq, null);

        // 3. Record an expense debit linked to bankAccount
        String expenseJson = String.format("""
                {
                    "bankAccountId": %d,
                    "amount": 2500.00,
                    "category": "STAFF_SALARY",
                    "paymentMode": "BANK_TRANSFER",
                    "referenceNumber": "SAL-OCT-01",
                    "paymentDate": "%s",
                    "description": "October security guard salary"
                }
                """, bankAccount.getId(), LocalDate.now().toString());

        mockMvc.perform(post("/api/transactions/expense")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson))
                .andExpect(status().isOk());

        // 4. Verify reconciliation:
        // Opening = 10,000
        // Credits = 4,500
        // Debits = 2,500
        // Calculated = 12,000
        // Current = 12,000
        // Discrepancy = 0
        // Reconciled = true
        mockMvc.perform(get("/api/bank-accounts/" + bankAccount.getId() + "/reconcile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(10000.00))
                .andExpect(jsonPath("$.totalCredits").value(4500.00))
                .andExpect(jsonPath("$.totalDebits").value(2500.00))
                .andExpect(jsonPath("$.calculatedBalance").value(12000.00))
                .andExpect(jsonPath("$.currentBalance").value(12000.00))
                .andExpect(jsonPath("$.discrepancy").value(0.00))
                .andExpect(jsonPath("$.reconciled").value(true));

        // 5. Test reconcile all
        mockMvc.perform(get("/api/bank-accounts/reconcile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
