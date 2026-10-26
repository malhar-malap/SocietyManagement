package com.society.jaswanti.controller;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.exception.DuplicateBillException;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.MaintenanceBillService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillUniquenessAndLedgerAuthorizationTests {

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
    private MaintenanceBillService billService;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User masterAdmin;
    private User residentA;
    private User residentB;
    private User residentC;
    private User formerResident;

    private Room room101;
    private Room room102;

    private String adminToken;
    private String masterAdminToken;
    private String residentAToken;
    private String residentBToken;
    private String residentCToken;
    private String formerResidentToken;

    private MaintenanceBill bill101Maint;
    private Transaction tx101;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        paymentAttemptRepository.deleteAll();
        transactionRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();

        admin = userRepository.findByEmail("admin_ledger@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Staff");
            u.setEmail("admin_ledger@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        masterAdmin = userRepository.findByEmail("master_admin_ledger@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Master Admin Committee");
            u.setEmail("master_admin_ledger@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.MASTER_ADMIN);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        residentA = userRepository.findByEmail("residentA@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident A Owner");
            u.setEmail("residentA@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        residentB = userRepository.findByEmail("residentB@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident B Tenant");
            u.setEmail("residentB@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        residentC = userRepository.findByEmail("residentC@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Resident C Other");
            u.setEmail("residentC@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        formerResident = userRepository.findByEmail("formerResident@jaswanti.com").orElseGet(() -> {
            User u = new User();
            u.setName("Former Resident");
            u.setEmail("formerResident@jaswanti.com");
            u.setPassword("$2a$10$hashedpassword");
            u.setRole(Role.RESIDENT);
            u.setStatus(UserStatus.ACTIVE);
            u.setActive(true);
            return userRepository.save(u);
        });

        adminToken = "Bearer " + jwtService.generateToken(admin);
        masterAdminToken = "Bearer " + jwtService.generateToken(masterAdmin);
        residentAToken = "Bearer " + jwtService.generateToken(residentA);
        residentBToken = "Bearer " + jwtService.generateToken(residentB);
        residentCToken = "Bearer " + jwtService.generateToken(residentC);
        formerResidentToken = "Bearer " + jwtService.generateToken(formerResident);

        room101 = roomRepository.findByRoomNumber("Z-101").orElseGet(() -> {
            Room r = new Room("Z-101", "Z", 1);
            return roomRepository.save(r);
        });

        room102 = roomRepository.findByRoomNumber("Z-102").orElseGet(() -> {
            Room r = new Room("Z-102", "Z", 1);
            return roomRepository.save(r);
        });

        // Room 101 has active Owner (A) and active Tenant (B), and inactive former occupant
        RoomResident relA = new RoomResident(room101, residentA, RelationshipType.OWNER, LocalDate.now().minusMonths(6), true);
        relA.setActive(true);
        roomResidentRepository.save(relA);

        RoomResident relB = new RoomResident(room101, residentB, RelationshipType.TENANT, LocalDate.now().minusMonths(2), false);
        relB.setActive(true);
        roomResidentRepository.save(relB);

        RoomResident relFormer = new RoomResident(room101, formerResident, RelationshipType.TENANT, LocalDate.now().minusMonths(12), false);
        relFormer.setEndDate(LocalDate.now().minusMonths(3));
        relFormer.setActive(false);
        roomResidentRepository.save(relFormer);

        // Room 102 has active Owner (C)
        RoomResident relC = new RoomResident(room102, residentC, RelationshipType.OWNER, LocalDate.now().minusMonths(4), true);
        relC.setActive(true);
        roomResidentRepository.save(relC);

        // Seed bill and transaction for Room 101
        bill101Maint = new MaintenanceBill(room101, "November 2026", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);
        bill101Maint = billRepository.save(bill101Maint);

        tx101 = new Transaction(bill101Maint, new BigDecimal("3500.00"), "SUCCESS", "CASH");
        tx101 = transactionRepository.save(tx101);
    }

    @Test
    @DisplayName("Multiple active occupants of a unit can view the unit ledger and bills")
    void testMultipleActiveOccupantsCanAccessSameUnitLedger() throws Exception {
        // Resident A (Owner of 101) can view room 101 bills and transactions
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", residentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", residentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", residentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(tx101.getId().intValue())));

        mockMvc.perform(get("/api/transactions/my-transactions")
                        .header("Authorization", residentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(tx101.getId().intValue())));

        // Resident B (Tenant of 101) can ALSO view room 101 bills and transactions
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", residentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", residentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", residentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(tx101.getId().intValue())));

        mockMvc.perform(get("/api/transactions/my-transactions")
                        .header("Authorization", residentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(tx101.getId().intValue())));
    }

    @Test
    @DisplayName("Resident denied access to another unit's bills and transactions")
    void testResidentDeniedAccessToAnotherUnitsBillsAndTransactions() throws Exception {
        // Resident C (Room 102 only) is denied access to Room 101
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", residentCToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bills/{id}", bill101Maint.getId())
                        .header("Authorization", residentCToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", residentCToken))
                .andExpect(status().isForbidden());

        // Resident C's my-bills does not contain room 101 bills
        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", residentCToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(bill101Maint.getId().intValue()))));

        // Resident C's my-transactions does not contain room 101 transactions
        mockMvc.perform(get("/api/transactions/my-transactions")
                        .header("Authorization", residentCToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(tx101.getId().intValue()))));
    }

    @Test
    @DisplayName("Former resident whose occupancy has ended is denied access to unit ledger")
    void testFormerResidentDeniedAccess() throws Exception {
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", formerResidentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bills/{id}", bill101Maint.getId())
                        .header("Authorization", formerResidentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", formerResidentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin and Master Admin retain full access to any unit ledger")
    void testAdminAndMasterAdminAccess() throws Exception {
        // Admin
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/bills/{id}", bill101Maint.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Master Admin
        mockMvc.perform(get("/api/bills/room/{roomId}", room101.getId())
                        .header("Authorization", masterAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(bill101Maint.getId().intValue())));

        mockMvc.perform(get("/api/bills/{id}", bill101Maint.getId())
                        .header("Authorization", masterAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/bill/{billId}", bill101Maint.getId())
                        .header("Authorization", masterAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Duplicate bill creation is rejected with 409 Conflict")
    void testDuplicateBillCreationRejection() throws Exception {
        // A bill already exists for Room 101, "November 2026", ChargeType.MAINTENANCE (saved in setUp)
        // Attempting to create a bill for canonical "2026-11" should detect logical equivalence and reject
        MaintenanceBill duplicateBill = new MaintenanceBill(room101, "2026-11", new BigDecimal("3500.00"), LocalDate.now().plusDays(10), ChargeType.MAINTENANCE);

        // Service level throws DuplicateBillException
        assertThatThrownBy(() -> billService.createBill(duplicateBill))
                .isInstanceOf(DuplicateBillException.class)
                .hasMessageContaining("A bill of type MAINTENANCE already exists for room");

        String json = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"2026-11\",\"amount\":3500.00,\"dueDate\":\"%s\",\"chargeType\":\"MAINTENANCE\"}",
                room101.getId(), LocalDate.now().plusDays(10));

        // Controller endpoint returns HTTP 409 Conflict
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("A bill of type MAINTENANCE already exists")));
    }

    @Test
    @DisplayName("Repeated monthly generation is idempotent and does not create duplicate bills")
    void testRepeatedMonthlyGenerationIsIdempotent() {
        String month = "2026-12";
        BigDecimal amount = new BigDecimal("3800.00");
        LocalDate dueDate = LocalDate.now().plusDays(15);

        long roomCount = roomRepository.count();
        assertThat(roomCount).isGreaterThan(0);

        // First generation creates bills for all rooms
        List<?> firstRun = billService.generateMonthlyBills(month, amount, dueDate, ChargeType.MAINTENANCE);
        assertThat(firstRun).hasSize((int) roomCount);

        long countAfterFirst = billRepository.findByBillingMonth(month).stream()
                .filter(b -> b.getChargeType() == ChargeType.MAINTENANCE)
                .count();
        assertThat(countAfterFirst).isEqualTo(roomCount);

        // Second generation for the exact same month & charge type should be a no-op (0 created)
        List<?> secondRun = billService.generateMonthlyBills(month, amount, dueDate, ChargeType.MAINTENANCE);
        assertThat(secondRun).isEmpty();

        long countAfterSecond = billRepository.findByBillingMonth(month).stream()
                .filter(b -> b.getChargeType() == ChargeType.MAINTENANCE)
                .count();
        assertThat(countAfterSecond).isEqualTo(roomCount);
    }

    @Test
    @DisplayName("Charge type separation allows different charge types for the same unit and month")
    void testChargeTypeSeparationAllowsDifferentChargesSameMonth() throws Exception {
        String month = "2026-11"; // Room 101 already has MAINTENANCE for this month (as "November 2026")

        // Creating SINKING_FUND succeeds
        String sinkingJson = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"%s\",\"amount\":500.00,\"dueDate\":\"%s\",\"chargeType\":\"SINKING_FUND\"}",
                room101.getId(), month, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinkingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeType", is("SINKING_FUND")));

        // Creating SPECIAL_ASSESSMENT succeeds
        String specialJson = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"%s\",\"amount\":2000.00,\"dueDate\":\"%s\",\"chargeType\":\"SPECIAL_ASSESSMENT\"}",
                room101.getId(), month, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeType", is("SPECIAL_ASSESSMENT")));

        // Creating PENALTY succeeds
        String penaltyJson = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"%s\",\"amount\":150.00,\"dueDate\":\"%s\",\"chargeType\":\"PENALTY\"}",
                room101.getId(), month, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(penaltyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeType", is("PENALTY")));

        // Creating a SECOND SINKING_FUND for the same month and unit fails with 409 Conflict
        String dupSinkingJson = String.format("{\"room\":{\"id\":%d},\"billingMonth\":\"%s\",\"amount\":600.00,\"dueDate\":\"%s\",\"chargeType\":\"SINKING_FUND\"}",
                room101.getId(), month, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/api/bills")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupSinkingJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("A bill of type SINKING_FUND already exists")));
    }
}
