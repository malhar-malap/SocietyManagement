package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private com.society.jaswanti.service.MaintenanceBillService billService;

    @Autowired
    private javax.sql.DataSource dataSource;

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
    // 6. Removed Legacy Razorpay Endpoints Return 404
    // ==========================================

    @Test
    @DisplayName("POST /api/payments/create-order is removed and returns 404")
    void testCreateOrderEndpointRemoved() throws Exception {
        mockMvc.perform(post("/api/payments/create-order")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billId\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/payments/verify is removed and returns 404")
    void testVerifyEndpointRemoved() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .header("Authorization", resident101Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billId\":1}"))
                .andExpect(status().isNotFound());
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
