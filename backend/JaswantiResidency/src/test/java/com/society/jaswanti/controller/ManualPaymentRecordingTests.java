package com.society.jaswanti.controller;

import com.society.jaswanti.dto.RecordPaymentRequest;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.ActivityLogService;
import com.society.jaswanti.service.PaymentRecordingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManualPaymentRecordingTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.society.jaswanti.support.TestDatabase testDatabase;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomResidentRepository roomResidentRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ReceiptSequenceRepository receiptSequenceRepository;

    @Autowired
    private PaymentRecordingService paymentRecordingService;

    @MockitoSpyBean
    private ActivityLogService activityLogService;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User masterAdmin;
    private User resident;
    private Room room101;
    private Room room102;
    private MaintenanceBill bill101;
    private MaintenanceBill bill102;

    private String adminToken;
    private String masterAdminToken;
    private String residentToken;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        paymentAttemptRepository.deleteAll();
        transactionRepository.deleteAll();
        activityLogRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();
        receiptSequenceRepository.deleteAll();

        admin = userRepository.findByEmail("admin_pay_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Pay");
            u.setEmail("admin_pay_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        masterAdmin = userRepository.findByEmail("master_admin_pay_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Master Admin Pay");
            u.setEmail("master_admin_pay_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.MASTER_ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        resident = userRepository.findByEmail("resident_pay_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident Pay");
            u.setEmail("resident_pay_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        adminToken = "Bearer " + jwtService.generateToken(admin);
        masterAdminToken = "Bearer " + jwtService.generateToken(masterAdmin);
        residentToken = "Bearer " + jwtService.generateToken(resident);

        room101 = roomRepository.findByRoomNumber("P-101").orElseGet(() -> {
            Room r = new Room("P-101", "P", 1);
            return roomRepository.save(r);
        });

        room102 = roomRepository.findByRoomNumber("P-102").orElseGet(() -> {
            Room r = new Room("P-102", "P", 1);
            return roomRepository.save(r);
        });

        RoomResident rr = new RoomResident(room101, resident, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        roomResidentRepository.save(rr);

        bill101 = new MaintenanceBill(room101, "2026-10", new BigDecimal("3500.00"), LocalDate.now().plusDays(15), ChargeType.MAINTENANCE);
        bill101 = billRepository.save(bill101);

        bill102 = new MaintenanceBill(room102, "2026-10", new BigDecimal("4000.00"), LocalDate.now().plusDays(15), ChargeType.MAINTENANCE);
        bill102 = billRepository.save(bill102);
    }

    @Test
    @DisplayName("Admin records full CASH payment successfully with receipt number and audit log")
    void testAdminPaymentSuccessCash() throws Exception {
        String json = String.format("""
                {
                    "amount": 3500.00,
                    "paymentMode": "CASH",
                    "paymentDate": "%s",
                    "notes": "Paid in full at society office"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId", is(bill101.getId().intValue())))
                .andExpect(jsonPath("$.amount", is(3500.00)))
                .andExpect(jsonPath("$.paymentMode", is("CASH")))
                .andExpect(jsonPath("$.billStatus", is("PAID_CASH")))
                .andExpect(jsonPath("$.paidAmount", is(3500.00)))
                .andExpect(jsonPath("$.remainingBalance", is(0.00)))
                .andExpect(jsonPath("$.receiptNumber", matchesPattern("^REC-\\d{6}-\\d{5}$")))
                .andExpect(jsonPath("$.recordedByName", is("Admin Pay")));

        // Verify database updates
        MaintenanceBill updatedBill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(updatedBill.getStatus()).isEqualTo(BillStatus.PAID_CASH);
        assertThat(updatedBill.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(updatedBill.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));

        List<Transaction> txs = transactionRepository.findByBillId(bill101.getId());
        assertThat(txs).hasSize(1);
        Transaction tx = txs.get(0);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(tx.getPaymentMode()).isEqualTo("CASH");
        assertThat(tx.getReceiptNumber()).isNotNull();
        assertThat(tx.getRecordedBy().getId()).isEqualTo(admin.getId());

        // Verify activity log
        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("RECORD_PAYMENT");
        assertThat(logs.get(0).getModule()).isEqualTo("LEDGER");
        assertThat(logs.get(0).getDescription()).contains(tx.getReceiptNumber());
        assertThat(logs.get(0).getDescription()).contains("Paid in full at society office");
    }

    @Test
    @DisplayName("Master Admin records BANK_TRANSFER payment successfully with reference number")
    void testMasterAdminPaymentSuccessBankTransfer() throws Exception {
        String json = String.format("""
                {
                    "amount": 4000.00,
                    "paymentMode": "BANK_TRANSFER",
                    "referenceNumber": "NEFT-20261021-998877",
                    "paymentDate": "%s",
                    "notes": "Direct bank transfer"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill102.getId() + "/record-payment")
                        .header("Authorization", masterAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId", is(bill102.getId().intValue())))
                .andExpect(jsonPath("$.amount", is(4000.00)))
                .andExpect(jsonPath("$.paymentMode", is("BANK_TRANSFER")))
                .andExpect(jsonPath("$.referenceNumber", is("NEFT-20261021-998877")))
                .andExpect(jsonPath("$.billStatus", is("PAID")))
                .andExpect(jsonPath("$.paidAmount", is(4000.00)))
                .andExpect(jsonPath("$.remainingBalance", is(0.00)))
                .andExpect(jsonPath("$.receiptNumber", matchesPattern("^REC-\\d{6}-\\d{5}$")))
                .andExpect(jsonPath("$.recordedByName", is("Master Admin Pay")));

        MaintenanceBill updatedBill = billRepository.findById(bill102.getId()).orElseThrow();
        assertThat(updatedBill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(updatedBill.getPaidAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    @DisplayName("Resident is rejected with HTTP 403 Forbidden")
    void testResidentRejection() throws Exception {
        String json = String.format("""
                {
                    "amount": 3500.00,
                    "paymentMode": "CASH",
                    "paymentDate": "%s"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Partial payment updates bill to PARTIALLY_PAID with correct balances")
    void testPartialPayment() throws Exception {
        String json = String.format("""
                {
                    "amount": 1500.00,
                    "paymentMode": "UPI_MANUAL",
                    "referenceNumber": "UPI-PART-001",
                    "paymentDate": "%s",
                    "notes": "First installment"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billStatus", is("PARTIALLY_PAID")))
                .andExpect(jsonPath("$.paidAmount", is(1500.00)))
                .andExpect(jsonPath("$.remainingBalance", is(2000.00)));

        MaintenanceBill updated = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BillStatus.PARTIALLY_PAID);
        assertThat(updated.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("Full payment after partial payment transitions bill to PAID and rejects further payments")
    void testFullPaymentAfterPartialPayment() throws Exception {
        // Step 1: Pay 1500.00
        String partialJson = String.format("""
                {
                    "amount": 1500.00,
                    "paymentMode": "CASH",
                    "paymentDate": "%s"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partialJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billStatus", is("PARTIALLY_PAID")));

        // Step 2: Pay remaining 2000.00
        String finalJson = String.format("""
                {
                    "amount": 2000.00,
                    "paymentMode": "CHEQUE",
                    "referenceNumber": "CHQ-889900",
                    "paymentDate": "%s"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billStatus", is("PAID")))
                .andExpect(jsonPath("$.paidAmount", is(3500.00)))
                .andExpect(jsonPath("$.remainingBalance", is(0.00)));

        MaintenanceBill updated = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(updated.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));

        // Step 3: Attempt third payment on fully paid bill -> rejected with 400
        String extraJson = String.format("""
                {
                    "amount": 500.00,
                    "paymentMode": "CASH",
                    "paymentDate": "%s"
                }
                """, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extraJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already fully paid")));
    }

    @Test
    @DisplayName("Invalid amounts (zero, negative, overpayment, excess scale) are rejected with 400")
    void testInvalidAmountsRejected() throws Exception {
        // Zero amount
        String zeroJson = String.format("{\"amount\":0.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());
        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroJson))
                .andExpect(status().isBadRequest());

        // Negative amount
        String negJson = String.format("{\"amount\":-100.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());
        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negJson))
                .andExpect(status().isBadRequest());

        // Overpayment (4000 on 3500 bill)
        String overJson = String.format("{\"amount\":4000.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());
        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds remaining balance")));

        // Scale > 2 (3500.555)
        String scaleJson = String.format("{\"amount\":3500.555,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());
        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scaleJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("more than 2 decimal places")));
    }

    @Test
    @DisplayName("Future payment date is rejected with 400 Bad Request")
    void testFuturePaymentDateRejected() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String json = String.format("{\"amount\":3500.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", tomorrow);

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be in the future")));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMode.class, names = {"BANK_TRANSFER", "CHEQUE", "UPI_MANUAL"})
    @DisplayName("Non-cash payment modes require a reference number")
    void testRequiredReferenceForNonCashModes(PaymentMode mode) throws Exception {
        String json = String.format("{\"amount\":3500.00,\"paymentMode\":\"%s\",\"paymentDate\":\"%s\"}", mode, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Reference number is required for " + mode)));
    }

    @Test
    @DisplayName("Duplicate request with Idempotency-Key header is idempotent and does not create duplicate transaction")
    void testIdempotentRetryWithHeader() throws Exception {
        String key = "IDEM-TEST-" + System.currentTimeMillis();
        String json = String.format("""
                {
                    "amount": 3500.00,
                    "paymentMode": "CASH",
                    "paymentDate": "%s",
                    "notes": "Idempotent payment test"
                }
                """, LocalDate.now());

        // First attempt
        String firstResponse = mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second attempt with exact same key and body
        String secondResponse = mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Must return the exact same receipt and transaction ID
        assertThat(firstResponse).isEqualTo(secondResponse);

        // Verify only 1 transaction was saved and bill was not double-credited
        List<Transaction> txs = transactionRepository.findByBillId(bill101.getId());
        assertThat(txs).hasSize(1);
        MaintenanceBill bill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(bill.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    @DisplayName("Reusing non-cash reference number across different bills is rejected with 409 Conflict")
    void testReusingNonCashReferenceRejectedWith409() throws Exception {
        String ref = "UTR-SHARED-112233";
        String json1 = String.format("""
                {
                    "amount": 3500.00,
                    "paymentMode": "BANK_TRANSFER",
                    "referenceNumber": "%s",
                    "paymentDate": "%s"
                }
                """, ref, LocalDate.now());

        // Bill 1 payment succeeds
        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isOk());

        // Bill 2 payment with same reference number is rejected
        String json2 = String.format("""
                {
                    "amount": 4000.00,
                    "paymentMode": "BANK_TRANSFER",
                    "referenceNumber": "%s",
                    "paymentDate": "%s"
                }
                """, ref, LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill102.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already been recorded for another payment")));
    }

    @Test
    @DisplayName("Receipt numbers are monotonically unique across sequential payments")
    void testReceiptNumberUniqueness() throws Exception {
        String json1 = String.format("{\"amount\":1000.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());
        String json2 = String.format("{\"amount\":1000.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());

        String res1 = mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String res2 = mockMvc.perform(post("/api/bills/" + bill102.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(res1).contains("REC-");
        assertThat(res2).contains("REC-");
        assertThat(res1).isNotEqualTo(res2);
    }

    @Test
    @DisplayName("Transaction rolls back cleanly when audit persistence fails")
    void testRollbackWhenAuditPersistenceFails() {
        RecordPaymentRequest request = new RecordPaymentRequest(
                new BigDecimal("2000.00"),
                PaymentMode.CASH,
                null,
                LocalDate.now(),
                "Rollback test payment"
        );

        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(admin.getEmail(), "password");

        // Simulate activityLogService failure
        doThrow(new RuntimeException("Simulated audit write failure"))
                .when(activityLogService).logActivity(any(), any(), any(), any());

        assertThrows(RuntimeException.class, () ->
                paymentRecordingService.recordPayment(bill101.getId(), null, request, adminAuth)
        );

        // Confirm rollback: bill status is still PENDING and paidAmount is 0.00
        MaintenanceBill persisted = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(persisted.getPaidAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(persisted.getBalance()).isEqualByComparingTo(new BigDecimal("3500.00"));

        // No transactions saved
        assertThat(transactionRepository.findByBillId(bill101.getId())).isEmpty();
    }

    @Test
    @DisplayName("Payment on reversed bill is rejected with 400 Bad Request")
    void testReversedBillPaymentRejected() throws Exception {
        bill101.setStatus(BillStatus.REVERSED);
        billRepository.save(bill101);

        String json = String.format("{\"amount\":3500.00,\"paymentMode\":\"CASH\",\"paymentDate\":\"%s\"}", LocalDate.now());

        mockMvc.perform(post("/api/bills/" + bill101.getId() + "/record-payment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot record payment for reversed bill")));
    }

    @Test
    @DisplayName("Legacy mark-paid-cash wrapper creates a fully audited payment with receipt and ActivityLog")
    void testLegacyMarkPaidCashCreatesAuditedPayment() throws Exception {
        mockMvc.perform(put("/api/bills/" + bill101.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Deprecated", containsString("deprecated")))
                .andExpect(jsonPath("$.status", is("PAID_CASH")))
                .andExpect(jsonPath("$.paidAmount", is(3500.00)))
                .andExpect(jsonPath("$.balance", is(0.00)));

        // Verify transaction was created through PaymentRecordingService
        List<Transaction> txs = transactionRepository.findByBillId(bill101.getId());
        assertThat(txs).hasSize(1);
        Transaction tx = txs.get(0);
        assertThat(tx.getPaymentMode()).isEqualTo("CASH");
        assertThat(tx.getReceiptNumber()).matches("^REC-\\d{6}-\\d{5}$");
        assertThat(tx.getRecordedBy().getId()).isEqualTo(admin.getId());
        assertThat(tx.getNotes()).contains("mark-paid-cash");

        // Verify ActivityLog entry was recorded
        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        ActivityLog log = logs.stream()
                .filter(l -> "RECORD_PAYMENT".equals(l.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(log.getModule()).isEqualTo("LEDGER");
        assertThat(log.getUser().getId()).isEqualTo(admin.getId());
        assertThat(log.getDescription()).contains(tx.getReceiptNumber());
    }

    @Test
    @DisplayName("Legacy mark-paid-cash is idempotent and cannot create duplicate payments or double-credit")
    void testLegacyMarkPaidCashIsIdempotent() throws Exception {
        // First call
        mockMvc.perform(put("/api/bills/" + bill101.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID_CASH")));

        // Second call on already paid bill
        mockMvc.perform(put("/api/bills/" + bill101.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID_CASH")));

        // Confirm only 1 transaction exists and paidAmount is not duplicated
        List<Transaction> txs = transactionRepository.findByBillId(bill101.getId());
        assertThat(txs).hasSize(1);
        MaintenanceBill bill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(bill.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));

        long auditCount = activityLogRepository.findAll().stream()
                .filter(l -> "RECORD_PAYMENT".equals(l.getAction()))
                .count();
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Legacy mark-paid-cash fails closed when resident attempts access")
    void testLegacyMarkPaidCashResidentForbidden() throws Exception {
        mockMvc.perform(put("/api/bills/" + bill101.getId() + "/mark-paid-cash")
                        .header("Authorization", residentToken))
                .andExpect(status().isForbidden());

        // Verify no changes occurred
        MaintenanceBill bill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(bill.getPaidAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(transactionRepository.findByBillId(bill101.getId())).isEmpty();
    }

    @Test
    @DisplayName("Legacy mark-paid-cash rolls back cleanly when audit log persistence fails")
    void testLegacyMarkPaidCashRollbackOnAuditFailure() {
        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(admin.getEmail(), "password");

        doThrow(new RuntimeException("Simulated audit write failure"))
                .when(activityLogService).logActivity(any(), any(), any(), any());

        assertThrows(RuntimeException.class, () ->
                paymentRecordingService.recordPayment(
                        bill101.getId(),
                        "LEGACY-CASH-" + bill101.getId(),
                        new com.society.jaswanti.dto.RecordPaymentRequest(
                                bill101.getBalance(),
                                PaymentMode.CASH,
                                null,
                                LocalDate.now(),
                                "Legacy cash payment via mark-paid-cash"
                        ),
                        adminAuth
                )
        );

        MaintenanceBill persisted = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(persisted.getPaidAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(transactionRepository.findByBillId(bill101.getId())).isEmpty();
    }
}
