package com.society.jaswanti.controller;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.MaintenanceBillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillingPeriodValidationTests {

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
    private com.society.jaswanti.repository.TransactionRepository transactionRepository;

    @Autowired
    private com.society.jaswanti.repository.PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private MaintenanceBillService billService;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User resident;
    private Room room1;
    private String adminToken;
    private String residentToken;

    @BeforeEach
    void setUp() {
        paymentAttemptRepository.deleteAll();
        transactionRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();

        admin = userRepository.findByEmail("admin_period_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Period");
            u.setEmail("admin_period_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        resident = userRepository.findByEmail("resident_period_test@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident Period");
            u.setEmail("resident_period_test@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        adminToken = "Bearer " + jwtService.generateToken(admin);
        residentToken = "Bearer " + jwtService.generateToken(resident);

        room1 = roomRepository.findByRoomNumber("BP-101").orElseGet(() -> {
            Room r = new Room("BP-101", "A", 1);
            return roomRepository.save(r);
        });

        RoomResident rr = new RoomResident(room1, resident, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        roomResidentRepository.save(rr);
    }

    @Test
    @DisplayName("Valid canonical YYYY-MM period creates bill successfully")
    void testValidCanonicalPeriodCreation() throws Exception {
        String json = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"2026-10\",\"amount\":3500.00,\"dueDate\":\"%s\",\"chargeType\":\"MAINTENANCE\"}",
                room1.getId(), LocalDate.now().plusDays(15));

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingMonth", is("2026-10")))
                .andExpect(jsonPath("$.chargeType", is("MAINTENANCE")))
                .andExpect(jsonPath("$.amount", is(3500.00)));
    }

    @Test
    @DisplayName("Valid canonical YYYY-MM period in batch generation succeeds")
    void testValidCanonicalPeriodBatchGeneration() throws Exception {
        mockMvc.perform(post("/api/bills/generate-monthly")
                        .header("Authorization", adminToken)
                        .param("billingMonth", "2026-10")
                        .param("amount", "3600.00")
                        .param("dueDate", LocalDate.now().plusDays(15).toString())
                        .param("chargeType", "MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].billingMonth", is("2026-10")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "October 2026",
            "Oct 2026",
            "2026-1",
            "2026/10",
            "10-2026",
            "invalid",
            "2026-13",
            "2026-00"
    })
    @DisplayName("Non-canonical or malformed billingMonth in createBill is rejected with 400 Bad Request")
    void testInvalidMonthFormatsRejectedInCreateBill(String invalidMonth) throws Exception {
        String json = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"%s\",\"amount\":3500.00,\"dueDate\":\"%s\",\"chargeType\":\"MAINTENANCE\"}",
                room1.getId(), invalidMonth, LocalDate.now().plusDays(15));

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("not in canonical format YYYY-MM")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "October 2026",
            "Oct 2026",
            "2026-1",
            "2026/10",
            "10-2026",
            "invalid",
            "2026-13",
            "2026-00"
    })
    @DisplayName("Non-canonical or malformed billingMonth in batch generation is rejected with 400 Bad Request")
    void testInvalidMonthFormatsRejectedInBatchGeneration(String invalidMonth) throws Exception {
        mockMvc.perform(post("/api/bills/generate-monthly")
                        .header("Authorization", adminToken)
                        .param("billingMonth", invalidMonth)
                        .param("amount", "3600.00")
                        .param("dueDate", LocalDate.now().plusDays(15).toString())
                        .param("chargeType", "MAINTENANCE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("not in canonical format YYYY-MM")));
    }

    @Test
    @DisplayName("Duplicate logical period rejected: legacy 'October 2026' in DB prevents creating canonical '2026-10'")
    void testDuplicateLogicalPeriodRejectedForLegacyRecord() throws Exception {
        // Save historical legacy bill directly to DB
        MaintenanceBill legacyBill = new MaintenanceBill(room1, "October 2026", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);
        billRepository.save(legacyBill);

        // Attempt to create canonical bill for same room and charge type
        String json = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"2026-10\",\"amount\":3500.00,\"dueDate\":\"%s\",\"chargeType\":\"MAINTENANCE\"}",
                room1.getId(), LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("A bill of type MAINTENANCE already exists")));
    }

    @Test
    @DisplayName("Batch generation idempotency respects legacy bill: does not generate duplicate for room with legacy record")
    void testBatchGenerationSkipsRoomsWithLegacyEquivalentRecord() {
        // Save historical legacy bill directly to DB for room1
        MaintenanceBill legacyBill = new MaintenanceBill(room1, "October 2026", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);
        billRepository.save(legacyBill);

        // Run batch generation for canonical '2026-10'
        List<?> generated = billService.generateMonthlyBills("2026-10", new BigDecimal("3500.00"), LocalDate.now().plusDays(15), ChargeType.MAINTENANCE);

        // Room 1 should NOT have a new bill generated
        long room1Bills = billRepository.findByRoomId(room1.getId()).stream()
                .filter(b -> b.getChargeType() == ChargeType.MAINTENANCE)
                .count();
        assertThat(room1Bills).isEqualTo(1); // Only the existing legacy one
    }

    @Test
    @DisplayName("Existing legacy bill can be retrieved via GET endpoints without mutation")
    void testLegacyBillCompatibilityGetEndpoints() throws Exception {
        MaintenanceBill legacyBill = new MaintenanceBill(room1, "October 2026", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);
        MaintenanceBill savedLegacy = billRepository.save(legacyBill);

        // 1. GET /api/bills/{id}
        mockMvc.perform(get("/api/bills/" + savedLegacy.getId())
                        .header("Authorization", residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedLegacy.getId().intValue())))
                .andExpect(jsonPath("$.billingMonth", is("October 2026")))
                .andExpect(jsonPath("$.chargeType", is("MAINTENANCE")));

        // 2. GET /api/bills/room/{roomId}
        mockMvc.perform(get("/api/bills/room/" + room1.getId())
                        .header("Authorization", residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].billingMonth", is("October 2026")));

        // 3. GET /api/bills/my-bills
        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].billingMonth", is("October 2026")));

        // Verify the database record itself was not mutated
        MaintenanceBill reloaded = billRepository.findById(savedLegacy.getId()).orElseThrow();
        assertThat(reloaded.getBillingMonth()).isEqualTo("October 2026");
    }

    @Test
    @DisplayName("Admin can mark legacy bill as paid in cash without format validation errors")
    void testLegacyBillMarkPaidCash() throws Exception {
        MaintenanceBill legacyBill = new MaintenanceBill(room1, "October 2026", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);
        MaintenanceBill savedLegacy = billRepository.save(legacyBill);

        mockMvc.perform(put("/api/bills/" + savedLegacy.getId() + "/mark-paid-cash")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID_CASH")))
                .andExpect(jsonPath("$.billingMonth", is("October 2026")));

        MaintenanceBill reloaded = billRepository.findById(savedLegacy.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BillStatus.PAID_CASH);
        assertThat(reloaded.getBillingMonth()).isEqualTo("October 2026");
    }
}
