package com.society.jaswanti.controller;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class ResidentPaymentSubmissionTests {

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
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ResidentPaymentSubmissionRepository submissionRepository;

    @Autowired
    private SocietyBankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtService jwtService;

    private User residentUser;
    private User otherResidentUser;
    private User adminUser;
    private Room room101;
    private Room room102;
    private SocietyBankAccount bankAccount;
    private MaintenanceBill bill101;

    private String residentToken;
    private String otherResidentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        submissionRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();
        roomRepository.deleteAll();
        bankAccountRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        residentUser = new User();
        residentUser.setName("Rahul Resident");
        residentUser.setEmail("rahul.sub@example.com");
        residentUser.setPassword("$2a$10$hashedpassword");
        residentUser.setRole(Role.RESIDENT);
        residentUser.setStatus(UserStatus.ACTIVE);
        residentUser.setActive(true);
        residentUser = userRepository.save(residentUser);

        otherResidentUser = new User();
        otherResidentUser.setName("Other Resident");
        otherResidentUser.setEmail("other.sub@example.com");
        otherResidentUser.setPassword("$2a$10$hashedpassword");
        otherResidentUser.setRole(Role.RESIDENT);
        otherResidentUser.setStatus(UserStatus.ACTIVE);
        otherResidentUser.setActive(true);
        otherResidentUser = userRepository.save(otherResidentUser);

        adminUser = new User();
        adminUser.setName("Suresh Admin");
        adminUser.setEmail("admin.sub@example.com");
        adminUser.setPassword("$2a$10$hashedpassword");
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // 2. Create Rooms
        room101 = new Room();
        room101.setRoomNumber("101");
        room101.setFloor(1);
        room101.setWing("A");
        room101.setResident(residentUser);
        room101 = roomRepository.save(room101);

        room102 = new Room();
        room102.setRoomNumber("102");
        room102.setFloor(1);
        room102.setWing("A");
        room102.setResident(otherResidentUser);
        room102 = roomRepository.save(room102);

        // 3. Link RoomResidents
        RoomResident rr1 = new RoomResident();
        rr1.setRoom(room101);
        rr1.setResident(residentUser);
        rr1.setRelationshipType(RelationshipType.OWNER);
        rr1.setActive(true);
        rr1.setStartDate(LocalDate.now());
        rr1.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr1);

        RoomResident rr2 = new RoomResident();
        rr2.setRoom(room102);
        rr2.setResident(otherResidentUser);
        rr2.setRelationshipType(RelationshipType.OWNER);
        rr2.setActive(true);
        rr2.setStartDate(LocalDate.now());
        rr2.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr2);

        // 4. Create Bank Account
        bankAccount = new SocietyBankAccount("Main Operational Bank", "HDFC Bank", "50100123456789", "HDFC0001234",
                SocietyBankAccountType.BANK, new BigDecimal("10000.00"));
        bankAccount = bankAccountRepository.save(bankAccount);

        // 5. Create Maintenance Bill for Room 101
        bill101 = new MaintenanceBill();
        bill101.setRoom(room101);
        bill101.setBillingMonth("2026-04");
        bill101.setAmount(new BigDecimal("3500.00"));
        bill101.setLateFee(BigDecimal.ZERO);
        bill101.setPaidAmount(BigDecimal.ZERO);
        bill101.setDueDate(LocalDate.of(2026, 4, 15));
        bill101.setStatus(BillStatus.PENDING);
        bill101.setChargeType(ChargeType.MAINTENANCE);
        bill101 = billRepository.save(bill101);

        residentToken = jwtService.generateToken(residentUser);
        otherResidentToken = jwtService.generateToken(otherResidentUser);
        adminToken = jwtService.generateToken(adminUser);
    }

    @Test
    @DisplayName("Resident can submit payment proof with screenshot for their own room")
    void residentSubmitPaymentProofSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "screenshot",
                "payment_proof.png",
                "image/png",
                "fake image content bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/payments/submissions")
                        .file(file)
                        .param("roomId", room101.getId().toString())
                        .param("amount", "3500.00")
                        .param("paymentMode", "UPI_MANUAL")
                        .param("referenceNumber", "UPI1234567890")
                        .param("residentDescription", "Paid April 2026 bill")
                        .param("selectedBillIds", bill101.getId().toString())
                        .param("advanceMonthsCount", "0")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(3500.00))
                .andExpect(jsonPath("$.paymentMode").value("UPI_MANUAL"))
                .andExpect(jsonPath("$.referenceNumber").value("UPI1234567890"));

        assertThat(submissionRepository.findAll()).hasSize(1);
        ResidentPaymentSubmission saved = submissionRepository.findAll().get(0);
        assertThat(saved.getScreenshotData()).isNotEmpty();
        assertThat(saved.getScreenshotFilename()).isEqualTo("payment_proof.png");
    }

    @Test
    @DisplayName("Resident is forbidden from submitting payment proof for another room")
    void residentCannotSubmitForOtherRoom() throws Exception {
        mockMvc.perform(multipart("/api/payments/submissions")
                        .param("roomId", room102.getId().toString())
                        .param("amount", "3500.00")
                        .param("paymentMode", "UPI_MANUAL")
                        .param("referenceNumber", "UPI999")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can view pending submissions and approve submission, crediting bank account")
    void adminApprovesSubmissionAndCreditsBank() throws Exception {
        // Create a pending submission
        ResidentPaymentSubmission sub = new ResidentPaymentSubmission();
        sub.setRoom(room101);
        sub.setSubmittedByUser(residentUser);
        sub.setAmount(new BigDecimal("3500.00"));
        sub.setPaymentMode("UPI_MANUAL");
        sub.setReferenceNumber("UPI-APPROVE-1");
        sub.setStatus("PENDING");
        sub.setAdvanceMonthsCount(0);
        sub.setSelectedBillIdsJson("[" + bill101.getId() + "]");
        sub.setCreatedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        // Admin checks list
        mockMvc.perform(get("/api/payments/submissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sub.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Admin approves
        String approveBody = String.format("""
                {
                    "bankAccountId": %d,
                    "receiptNumber": "REC-2026-SUB-1",
                    "notes": "Verified in HDFC netbanking"
                }
                """, bankAccount.getId());

        mockMvc.perform(post("/api/payments/submissions/" + sub.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.receiptNumber").value("REC-2026-SUB-1"));

        // Verify Bill is now PAID
        MaintenanceBill updatedBill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(updatedBill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(updatedBill.getPaidAmount()).isEqualByComparingTo("3500.00");

        // Verify Bank Account balance is credited: 10000 + 3500 = 13500
        SocietyBankAccount updatedBank = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("13500.00");

        // Verify Transaction record exists
        assertThat(transactionRepository.findAll()).hasSize(1);
        Transaction tx = transactionRepository.findAll().get(0);
        assertThat(tx.getBankAccount().getId()).isEqualTo(bankAccount.getId());
        assertThat(tx.getAmount()).isEqualByComparingTo("3500.00");
        assertThat(tx.getEntryDirection()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("Admin can reject submission with reason without altering bank balance or bill")
    void adminRejectsSubmissionWithReason() throws Exception {
        ResidentPaymentSubmission sub = new ResidentPaymentSubmission();
        sub.setRoom(room101);
        sub.setSubmittedByUser(residentUser);
        sub.setAmount(new BigDecimal("3500.00"));
        sub.setPaymentMode("UPI_MANUAL");
        sub.setReferenceNumber("INVALID-UTR");
        sub.setStatus("PENDING");
        sub.setCreatedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        String rejectBody = """
                {
                    "reason": "UTR did not reflect in bank statement"
                }
                """;

        mockMvc.perform(post("/api/payments/submissions/" + sub.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("UTR did not reflect in bank statement"));

        // Bank balance unchanged
        SocietyBankAccount unchangedBank = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(unchangedBank.getCurrentBalance()).isEqualByComparingTo("10000.00");

        // Bill unchanged
        MaintenanceBill unchangedBill = billRepository.findById(bill101.getId()).orElseThrow();
        assertThat(unchangedBill.getStatus()).isEqualTo(BillStatus.PENDING);
    }

    @Test
    @DisplayName("Admin approving submission with advance months creates and pays advance bills")
    void adminApprovesAdvanceSubmissionGeneratesAdvanceBills() throws Exception {
        // Resident pays for current month (3500) + 2 months advance (2 * 3500 = 7000) = 10500 total
        ResidentPaymentSubmission sub = new ResidentPaymentSubmission();
        sub.setRoom(room101);
        sub.setSubmittedByUser(residentUser);
        sub.setAmount(new BigDecimal("10500.00"));
        sub.setPaymentMode("BANK_TRANSFER");
        sub.setReferenceNumber("NEFT-ADV-1");
        sub.setStatus("PENDING");
        sub.setAdvanceMonthsCount(2);
        sub.setSelectedBillIdsJson("[" + bill101.getId() + "]");
        sub.setCreatedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        String approveBody = String.format("""
                {
                    "bankAccountId": %d
                }
                """, bankAccount.getId());

        mockMvc.perform(post("/api/payments/submissions/" + sub.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Total bills for room101 should now be 3 (current 2026-04, plus 2026-05 and 2026-06)
        var allBills = billRepository.findByRoomIdOrderByBillingMonthDesc(room101.getId());
        assertThat(allBills).hasSize(3);
        assertThat(allBills).allMatch(b -> b.getStatus() == BillStatus.PAID);

        // Bank balance should be credited by 10500 (10000 + 10500 = 20500)
        SocietyBankAccount updatedBank = bankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("20500.00");
    }

    @Test
    @DisplayName("Resident A cannot read Resident B payment screenshot, owner, co-resident, admin, and master admin can")
    void testScreenshotAccessPermissionsAndIdor() throws Exception {
        // Create co-resident in room 101
        User coResident = new User();
        coResident.setName("Co Resident");
        coResident.setEmail("co.resident@example.com");
        coResident.setPassword("$2a$10$hashedpassword");
        coResident.setRole(Role.RESIDENT);
        coResident.setStatus(UserStatus.ACTIVE);
        coResident.setActive(true);
        coResident = userRepository.save(coResident);

        RoomResident rrCo = new RoomResident();
        rrCo.setRoom(room101);
        rrCo.setResident(coResident);
        rrCo.setRelationshipType(RelationshipType.TENANT);
        rrCo.setActive(true);
        rrCo.setStartDate(LocalDate.now());
        rrCo.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rrCo);

        // Create master admin
        User masterAdmin = new User();
        masterAdmin.setName("Master Admin User");
        masterAdmin.setEmail("master.admin@example.com");
        masterAdmin.setPassword("$2a$10$hashedpassword");
        masterAdmin.setRole(Role.MASTER_ADMIN);
        masterAdmin.setStatus(UserStatus.ACTIVE);
        masterAdmin.setActive(true);
        masterAdmin = userRepository.save(masterAdmin);

        String coResidentToken = jwtService.generateToken(coResident);
        String masterAdminToken = jwtService.generateToken(masterAdmin);

        // Create submission by residentUser for room101
        ResidentPaymentSubmission sub = new ResidentPaymentSubmission();
        sub.setRoom(room101);
        sub.setSubmittedByUser(residentUser);
        sub.setAmount(new BigDecimal("3500.00"));
        sub.setPaymentMode("UPI");
        sub.setStatus("PENDING");
        sub.setScreenshotData("fake screenshot bytes".getBytes());
        sub.setScreenshotContentType("image/png");
        sub.setScreenshotFilename("proof.png");
        sub.setCreatedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        // 1. Other resident B (not in room 101, not creator) -> 403 Forbidden
        mockMvc.perform(get("/api/payments/submissions/" + sub.getId() + "/screenshot")
                        .header("Authorization", "Bearer " + otherResidentToken))
                .andExpect(status().isForbidden());

        // 2. Owner resident A -> 200 OK
        mockMvc.perform(get("/api/payments/submissions/" + sub.getId() + "/screenshot")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        // 3. Co-resident in room 101 -> 200 OK
        mockMvc.perform(get("/api/payments/submissions/" + sub.getId() + "/screenshot")
                        .header("Authorization", "Bearer " + coResidentToken))
                .andExpect(status().isOk());

        // 4. Admin -> 200 OK
        mockMvc.perform(get("/api/payments/submissions/" + sub.getId() + "/screenshot")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 5. Master Admin -> 200 OK
        mockMvc.perform(get("/api/payments/submissions/" + sub.getId() + "/screenshot")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk());

        // 6. Missing submission -> 404 Not Found for both resident and admin
        mockMvc.perform(get("/api/payments/submissions/99999/screenshot")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/payments/submissions/99999/screenshot")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
