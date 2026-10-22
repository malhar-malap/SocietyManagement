package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.dto.PaymentRequestDto;
import com.society.jaswanti.dto.PaymentVerifyDto;
import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.TransactionRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthorizationBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private TransactionRepository transactionRepository;

    @Autowired
    private com.society.jaswanti.repository.PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private com.society.jaswanti.service.PaymentService paymentService;

    @Autowired
    private com.society.jaswanti.service.MaintenanceBillService billService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${razorpay.api.secret}")
    private String razorpaySecret;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String resident101Token;
    private String resident102Token;

    private Long resident101UserId;
    private Long resident102UserId;
    private Long room101Id;
    private Long room102Id;
    private Long bill101Id;
    private Long bill102Id;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByEmail("admin@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin@jaswanti.com");
            u.setName("Admin User");
            u.setRole(Role.ADMIN);
            u.setPassword("$2a$10$hashedpassword");
            u.setPhone("9999999999");
            return userRepository.save(u);
        });

        User resident101 = userRepository.findByEmail("resident101@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("resident101@jaswanti.com");
            u.setName("Resident 101");
            u.setRole(Role.RESIDENT);
            u.setPassword("$2a$10$hashedpassword");
            u.setPhone("9888888888");
            return userRepository.save(u);
        });

        User resident102 = userRepository.findByEmail("resident102@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("resident102@jaswanti.com");
            u.setName("Resident 102");
            u.setRole(Role.RESIDENT);
            u.setPassword("$2a$10$hashedpassword");
            u.setPhone("9777777777");
            return userRepository.save(u);
        });

        resident101UserId = resident101.getId();
        resident102UserId = resident102.getId();

        adminToken = "Bearer " + jwtService.generateToken(admin);
        resident101Token = "Bearer " + jwtService.generateToken(resident101);
        resident102Token = "Bearer " + jwtService.generateToken(resident102);

        Room room101 = roomRepository.findByRoomNumber("C-101").orElseGet(() -> {
            Room r = new Room("C-101", "C", 1);
            r.setResident(resident101);
            return roomRepository.save(r);
        });
        if (room101.getResident() == null || !room101.getResident().getId().equals(resident101UserId)) {
            room101.setResident(resident101);
            roomRepository.save(room101);
        }
        room101Id = room101.getId();

        Room room102 = roomRepository.findByRoomNumber("C-102").orElseGet(() -> {
            Room r = new Room("C-102", "C", 1);
            r.setResident(resident102);
            return roomRepository.save(r);
        });
        if (room102.getResident() == null || !room102.getResident().getId().equals(resident102UserId)) {
            room102.setResident(resident102);
            roomRepository.save(room102);
        }
        room102Id = room102.getId();

        MaintenanceBill bill101 = billRepository.findByRoomId(room101Id).stream()
                .findFirst()
                .orElseGet(() -> {
                    MaintenanceBill b = new MaintenanceBill(room101, "October 2026", 3500.0, LocalDate.now().plusDays(10));
                    b.setStatus(BillStatus.PENDING);
                    return billRepository.save(b);
                });
        bill101Id = bill101.getId();

        MaintenanceBill bill102 = billRepository.findByRoomId(room102Id).stream()
                .findFirst()
                .orElseGet(() -> {
                    MaintenanceBill b = new MaintenanceBill(room102, "October 2026", 3500.0, LocalDate.now().plusDays(10));
                    b.setStatus(BillStatus.PENDING);
                    return billRepository.save(b);
                });
        bill102Id = bill102.getId();

        if (transactionRepository.findByBillId(bill101Id).isEmpty()) {
            Transaction t1 = new Transaction(bill101, 3500.0, "SUCCESS", "CASH");
            transactionRepository.save(t1);
        }
    }

    // ==========================================
    // 1. All-Bills Endpoint Access Control
    // ==========================================

    @Test
    @DisplayName("Resident must NOT access society-wide GET /api/bills")
    void testResidentCannotAccessAllBills() throws Exception {
        mockMvc.perform(get("/api/bills")
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin CAN access society-wide GET /api/bills")
    void testAdminCanAccessAllBills() throws Exception {
        mockMvc.perform(get("/api/bills")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. Room Enumeration Access Control
    // ==========================================

    @Test
    @DisplayName("Resident must NOT access society-wide GET /api/rooms")
    void testResidentCannotAccessAllRooms() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin CAN access society-wide GET /api/rooms")
    void testAdminCanAccessAllRooms() throws Exception {
        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 3. Resident vs Another Resident's Rooms
    // ==========================================

    @Test
    @DisplayName("Resident 101 must NOT access Resident 102 rooms via GET /api/rooms/resident/{id}")
    void testResidentCannotAccessOtherResidentRooms() throws Exception {
        mockMvc.perform(get("/api/rooms/resident/" + resident102UserId)
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 CAN access own rooms via GET /api/rooms/resident/{id}")
    void testResidentCanAccessOwnRooms() throws Exception {
        mockMvc.perform(get("/api/rooms/resident/" + resident101UserId)
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Resident 101 CAN access own rooms via GET /api/rooms/my-rooms")
    void testResidentCanAccessMyRooms() throws Exception {
        mockMvc.perform(get("/api/rooms/my-rooms")
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 4. Resident vs Another Resident's Bills
    // ==========================================

    @Test
    @DisplayName("Resident 101 must NOT access Room 102 bills via GET /api/bills/room/{roomId}")
    void testResidentCannotAccessOtherRoomBills() throws Exception {
        mockMvc.perform(get("/api/bills/room/" + room102Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 CAN access own Room 101 bills via GET /api/bills/room/{roomId}")
    void testResidentCanAccessOwnRoomBills() throws Exception {
        mockMvc.perform(get("/api/bills/room/" + room101Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Resident 101 must NOT access Bill 102 by ID via GET /api/bills/{id}")
    void testResidentCannotAccessOtherBillById() throws Exception {
        mockMvc.perform(get("/api/bills/" + bill102Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 CAN access own Bill 101 by ID via GET /api/bills/{id}")
    void testResidentCanAccessOwnBillById() throws Exception {
        mockMvc.perform(get("/api/bills/" + bill101Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Resident 101 CAN access own bills via GET /api/bills/my-bills")
    void testResidentCanAccessMyBills() throws Exception {
        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 5. Transaction Access Control
    // ==========================================

    @Test
    @DisplayName("Resident must NOT access society-wide GET /api/transactions")
    void testResidentCannotAccessAllTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin CAN access society-wide GET /api/transactions")
    void testAdminCanAccessAllTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Resident 101 must NOT access Bill 102 transactions via GET /api/transactions/bill/{billId}")
    void testResidentCannotAccessOtherBillTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions/bill/" + bill102Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 CAN access own Bill 101 transactions via GET /api/transactions/bill/{billId}")
    void testResidentCanAccessOwnBillTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions/bill/" + bill101Id)
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Resident 101 CAN access own transactions via GET /api/transactions/my-transactions")
    void testResidentCanAccessMyTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions/my-transactions")
                        .header("Authorization", resident101Token))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 6. Payment Verification & Order Creation
    // ==========================================

    @Test
    @DisplayName("POST /api/payments/verify rejects missing billId with 400 Bad Request")
    void testPaymentVerifyRejectsMissingBillId() throws Exception {
        PaymentVerifyDto dto = new PaymentVerifyDto();
        dto.setRazorpayOrderId("order_test_123");
        dto.setRazorpayPaymentId("pay_test_123");
        dto.setRazorpaySignature("signature_123");
        dto.setBillId(null);

        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payments/verify rejects unauthorized billId with 403 Forbidden")
    void testPaymentVerifyRejectsUnauthorizedBillId() throws Exception {
        PaymentVerifyDto dto = new PaymentVerifyDto();
        dto.setRazorpayOrderId("order_test_123");
        dto.setRazorpayPaymentId("pay_test_123");
        dto.setRazorpaySignature("signature_123");
        dto.setBillId(bill102Id); // belongs to resident 102

        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/payments/create-order rejects unauthorized billId with 403 Forbidden")
    void testPaymentCreateOrderRejectsUnauthorizedBillId() throws Exception {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setBillId(bill102Id); // belongs to resident 102

        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 7. Dashboard ID Spoofing Protection
    // ==========================================

    @Test
    @DisplayName("Resident 101 must NOT spoof Resident 102 ID on GET /api/resident/dashboard")
    void testResidentCannotSpoofOtherResidentOnDashboard() throws Exception {
        mockMvc.perform(get("/api/resident/dashboard")
                        .header("Authorization", resident101Token)
                        .param("residentId", String.valueOf(resident102UserId))
                        .param("roomId", String.valueOf(room101Id)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 must NOT spoof Room 102 on GET /api/resident/dashboard")
    void testResidentCannotSpoofOtherRoomOnDashboard() throws Exception {
        mockMvc.perform(get("/api/resident/dashboard")
                        .header("Authorization", resident101Token)
                        .param("residentId", String.valueOf(resident101UserId))
                        .param("roomId", String.valueOf(room102Id)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Resident 101 CAN access own dashboard on GET /api/resident/dashboard")
    void testResidentCanAccessOwnDashboard() throws Exception {
        mockMvc.perform(get("/api/resident/dashboard")
                        .header("Authorization", resident101Token)
                        .param("residentId", String.valueOf(resident101UserId))
                        .param("roomId", String.valueOf(room101Id)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 8. Unauthenticated Access Rejection (401)
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to protected endpoint must return 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/bills"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 9. Cash Payment Boundary & Idempotency Tests
    // ==========================================

    private String calculateSignature(String orderId, String paymentId) throws Exception {
        String payload = orderId + "|" + paymentId;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(razorpaySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    @DisplayName("Online payment verification must NOT change a cash-settled bill (PAID_CASH) or create duplicate transaction")
    void testPaymentVerifyRejectsCashPaidBill() throws Exception {
        MaintenanceBill cashBill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "September 2026",
                3500.0,
                LocalDate.now().plusDays(5)
        );
        cashBill.setStatus(BillStatus.PAID_CASH);
        cashBill = billRepository.save(cashBill);

        Transaction cashTx = new Transaction(cashBill, 3500.0, "SUCCESS", "CASH");
        cashTx.setGatewayPaymentId("CASH_TEST_123");
        transactionRepository.save(cashTx);

        String orderId = "order_cash_test_999";
        String paymentId = "pay_cash_test_999";
        com.society.jaswanti.entity.PaymentAttempt attempt = new com.society.jaswanti.entity.PaymentAttempt(cashBill, orderId, 3500.0);
        paymentAttemptRepository.save(attempt);

        String signature = calculateSignature(orderId, paymentId);

        PaymentVerifyDto verifyDto = new PaymentVerifyDto();
        verifyDto.setRazorpayOrderId(orderId);
        verifyDto.setRazorpayPaymentId(paymentId);
        verifyDto.setRazorpaySignature(signature);
        verifyDto.setBillId(cashBill.getId());

        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isBadRequest());

        // Verify bill status is unchanged
        MaintenanceBill reloadedBill = billRepository.findById(cashBill.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.PAID_CASH, reloadedBill.getStatus());

        // Verify only 1 cash transaction exists
        List<Transaction> transactions = transactionRepository.findByBillId(cashBill.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, transactions.size());
        org.junit.jupiter.api.Assertions.assertEquals("CASH", transactions.get(0).getPaymentMode());
    }

    @Test
    @DisplayName("Online order creation must reject a cash-settled bill (PAID_CASH)")
    void testCreateOrderRejectsCashPaidBill() throws Exception {
        MaintenanceBill cashBill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "August 2026",
                3500.0,
                LocalDate.now().plusDays(5)
        );
        cashBill.setStatus(BillStatus.PAID_CASH);
        cashBill = billRepository.save(cashBill);

        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setBillId(cashBill.getId());

        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin marking bill paid cash sets PAID_CASH status and is idempotent")
    void testAdminMarkPaidCashSetsStatusAndIsIdempotent() throws Exception {
        MaintenanceBill pendingBill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "July 2026",
                3500.0,
                LocalDate.now().plusDays(5)
        );
        pendingBill.setStatus(BillStatus.PENDING);
        pendingBill = billRepository.save(pendingBill);

        // First mark paid cash
        mockMvc.perform(put("/api/bills/" + pendingBill.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID_CASH"));

        MaintenanceBill afterFirst = billRepository.findById(pendingBill.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.PAID_CASH, afterFirst.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(1, transactionRepository.findByBillId(pendingBill.getId()).size());

        // Second mark paid cash (idempotent)
        mockMvc.perform(put("/api/bills/" + pendingBill.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1, transactionRepository.findByBillId(pendingBill.getId()).size());
    }

    @Test
    @DisplayName("Repeated verification for the same recorded payment succeeds without duplicate transactions")
    void testRepeatedSamePaymentVerificationSucceedsWithoutDuplicate() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "November 2026",
                3500.0,
                LocalDate.now().plusDays(10)
        );
        bill.setStatus(BillStatus.PENDING);
        bill = billRepository.save(bill);

        String orderId = "order_repeat_111";
        String paymentId = "pay_repeat_111";
        com.society.jaswanti.entity.PaymentAttempt attempt = new com.society.jaswanti.entity.PaymentAttempt(bill, orderId, 3500.0);
        paymentAttemptRepository.save(attempt);

        String signature = calculateSignature(orderId, paymentId);

        PaymentVerifyDto verifyDto = new PaymentVerifyDto();
        verifyDto.setRazorpayOrderId(orderId);
        verifyDto.setRazorpayPaymentId(paymentId);
        verifyDto.setRazorpaySignature(signature);
        verifyDto.setBillId(bill.getId());

        // First verification: should succeed
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk());

        MaintenanceBill afterFirst = billRepository.findById(bill.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.PAID, afterFirst.getStatus());
        List<Transaction> txsAfterFirst = transactionRepository.findByBillId(bill.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, txsAfterFirst.size());
        org.junit.jupiter.api.Assertions.assertEquals(paymentId, txsAfterFirst.get(0).getGatewayPaymentId());

        // Second verification with exact same payment: should succeed idempotently
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk());

        // Verify still exactly 1 transaction
        List<Transaction> txsAfterSecond = transactionRepository.findByBillId(bill.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, txsAfterSecond.size());
    }

    @Test
    @DisplayName("A different payment attempt against an already-paid bill must not be reported as successful")
    void testDifferentPaymentAgainstPaidBillRejected() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "December 2026",
                3500.0,
                LocalDate.now().plusDays(10)
        );
        bill.setStatus(BillStatus.PENDING);
        bill = billRepository.save(bill);

        String orderId1 = "order_diff_1";
        String paymentId1 = "pay_diff_1";
        com.society.jaswanti.entity.PaymentAttempt attempt1 = new com.society.jaswanti.entity.PaymentAttempt(bill, orderId1, 3500.0);
        paymentAttemptRepository.save(attempt1);

        String signature1 = calculateSignature(orderId1, paymentId1);

        PaymentVerifyDto verifyDto1 = new PaymentVerifyDto();
        verifyDto1.setRazorpayOrderId(orderId1);
        verifyDto1.setRazorpayPaymentId(paymentId1);
        verifyDto1.setRazorpaySignature(signature1);
        verifyDto1.setBillId(bill.getId());

        // First payment succeeds
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto1)))
                .andExpect(status().isOk());

        // Create second attempt for the same bill
        String orderId2 = "order_diff_2";
        String paymentId2 = "pay_diff_2";
        com.society.jaswanti.entity.PaymentAttempt attempt2 = new com.society.jaswanti.entity.PaymentAttempt(bill, orderId2, 3500.0);
        paymentAttemptRepository.save(attempt2);

        String signature2 = calculateSignature(orderId2, paymentId2);

        PaymentVerifyDto verifyDto2 = new PaymentVerifyDto();
        verifyDto2.setRazorpayOrderId(orderId2);
        verifyDto2.setRazorpayPaymentId(paymentId2);
        verifyDto2.setRazorpaySignature(signature2);
        verifyDto2.setBillId(bill.getId());

        // Second DIFFERENT payment attempt against the already-paid bill must fail (400 Bad Request)
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto2)))
                .andExpect(status().isBadRequest());

        // Assert only transaction 1 is recorded, paymentId2 is never recorded
        List<Transaction> txs = transactionRepository.findByBillId(bill.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, txs.size());
        org.junit.jupiter.api.Assertions.assertEquals(paymentId1, txs.get(0).getGatewayPaymentId());
        org.junit.jupiter.api.Assertions.assertTrue(transactionRepository.findByGatewayPaymentId(paymentId2).isEmpty());
    }

    @Test
    @DisplayName("Rollback/Atomicity: If transaction persistence fails, cash bill settlement rolls back")
    void testCashSettlementRollsBackOnTransactionFailure() {
        MaintenanceBill bill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "February 2027",
                3500.0,
                LocalDate.now().plusDays(10)
        );
        bill.setStatus(BillStatus.PENDING);
        MaintenanceBill savedBill = billRepository.save(bill);
        final Long targetBillId = savedBill.getId();

        org.mockito.Mockito.doThrow(new RuntimeException("Simulated database failure during transaction save"))
                .when(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(t ->
                        t != null && t.getBill() != null && targetBillId.equals(t.getBill().getId())
                ));

        try {
            billService.markPaidCash(targetBillId);
        } catch (Exception expected) {
            // Expected rollback
        }

        // Verify that bill status rolled back and remains PENDING
        MaintenanceBill reloaded = billRepository.findById(targetBillId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.PENDING, reloaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, transactionRepository.findByBillId(targetBillId).size());
    }

    @Test
    @DisplayName("Rollback/Atomicity: If transaction persistence fails, online bill settlement rolls back")
    void testOnlineSettlementRollsBackOnTransactionFailure() throws Exception {
        MaintenanceBill bill = new MaintenanceBill(
                roomRepository.findById(room101Id).orElseThrow(),
                "March 2027",
                3500.0,
                LocalDate.now().plusDays(10)
        );
        bill.setStatus(BillStatus.PENDING);
        MaintenanceBill savedBill = billRepository.save(bill);
        final Long targetBillId = savedBill.getId();

        String orderId = "order_rollback_1";
        String paymentId = "pay_rollback_1";
        com.society.jaswanti.entity.PaymentAttempt attempt = new com.society.jaswanti.entity.PaymentAttempt(savedBill, orderId, 3500.0);
        paymentAttemptRepository.save(attempt);

        String signature = calculateSignature(orderId, paymentId);

        PaymentVerifyDto verifyDto = new PaymentVerifyDto();
        verifyDto.setRazorpayOrderId(orderId);
        verifyDto.setRazorpayPaymentId(paymentId);
        verifyDto.setRazorpaySignature(signature);
        verifyDto.setBillId(targetBillId);

        org.mockito.Mockito.doThrow(new RuntimeException("Simulated DB failure during online transaction save"))
                .when(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(t ->
                        t != null && t.getBill() != null && targetBillId.equals(t.getBill().getId())
                ));

        boolean result = paymentService.verifyPayment(verifyDto);
        org.junit.jupiter.api.Assertions.assertFalse(result);

        // Verify that bill status rolled back and remains PENDING
        MaintenanceBill reloaded = billRepository.findById(targetBillId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(BillStatus.PENDING, reloaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, transactionRepository.findByBillId(targetBillId).size());
    }

    // ==========================================
    // 10. Database Isolation Verification
    // ==========================================

    @Test
    @DisplayName("Test suite must run against isolated test database, not development database")
    void testDatabaseIsolation() throws Exception {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            org.junit.jupiter.api.Assertions.assertTrue(
                    dbUrl.contains("h2:mem") || dbUrl.contains("test"),
                    "Expected isolated test database URL but was: " + dbUrl
            );
        }
    }
}
