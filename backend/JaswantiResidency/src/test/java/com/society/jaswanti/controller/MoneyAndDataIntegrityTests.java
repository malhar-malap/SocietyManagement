package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MoneyAndDataIntegrityTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomResidentRepository roomResidentRepository;
    @Autowired private MaintenanceBillRepository billRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private ResidentPaymentSubmissionRepository submissionRepository;
    @Autowired private SocietyBankAccountRepository bankAccountRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private ActivityLogRepository activityLogRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private TransactionAmendmentRequestRepository amendmentRequestRepository;
    @Autowired private SystemNotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User admin;
    private User residentA;
    private User residentB;
    private Room roomA;
    private Room roomB;
    private MaintenanceBill billA;
    private MaintenanceBill billB;
    private SocietyBankAccount bank;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        notificationRepository.deleteAll();
        amendmentRequestRepository.deleteAll();
        submissionRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        billRepository.deleteAll();
        complaintRepository.deleteAll();
        announcementRepository.deleteAll();
        roomResidentRepository.deleteAll();
        roomRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();

        admin = user("money-admin@example.test", Role.ADMIN, true);
        residentA = user("money-a@example.test", Role.RESIDENT, true);
        residentB = user("money-b@example.test", Role.RESIDENT, true);
        user("money-inactive@example.test", Role.RESIDENT, false);

        roomA = room("A-1", residentA);
        roomB = room("B-1", residentB);
        bank = bankAccountRepository.save(new SocietyBankAccount("Main", "HDFC", "123", "HDFC0001",
                SocietyBankAccountType.BANK, new BigDecimal("0.00")));
        billA = bill(roomA, "2026-04", "3500.00", BillStatus.PENDING, "0.00");
        billB = bill(roomB, "2026-04", "3500.00", BillStatus.PENDING, "0.00");
    }

    private User user(String email, Role role, boolean active) {
        User u = new User();
        u.setEmail(email);
        u.setName("User " + email);
        u.setPassword("$2a$10$hashedpassword");
        u.setRole(role);
        u.setStatus(active ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        u.setActive(active);
        u.setForcePasswordChange(false);
        return userRepository.save(u);
    }

    private Room room(String number, User resident) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setWing(number.substring(0, 1));
        r.setFloor(1);
        r.setResident(resident);
        r = roomRepository.save(r);
        RoomResident rr = new RoomResident();
        rr.setRoom(r);
        rr.setResident(resident);
        rr.setRelationshipType(RelationshipType.OWNER);
        rr.setActive(true);
        rr.setStartDate(LocalDate.now().minusYears(1));
        rr.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr);
        return r;
    }

    private MaintenanceBill bill(Room room, String month, String amount, BillStatus status, String paid) {
        MaintenanceBill b = new MaintenanceBill();
        b.setRoom(room);
        b.setBillingMonth(month);
        b.setAmount(new BigDecimal(amount));
        b.setLateFee(BigDecimal.ZERO);
        b.setPaidAmount(new BigDecimal(paid));
        b.setDueDate(LocalDate.of(2026, 4, 15));
        b.setStatus(status);
        b.setChargeType(ChargeType.MAINTENANCE);
        return billRepository.save(b);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    // ---------- Bills ----------

    @Test
    @DisplayName("Creating a bill ignores a client-supplied id, status and paid amount")
    void createBillIgnoresLedgerFields() throws Exception {
        String body = "{\"id\":" + billA.getId() + ",\"room\":{\"id\":" + roomA.getId() + "},\"billingMonth\":\"2026-05\","
                + "\"amount\":3500,\"dueDate\":\"2026-05-15\",\"status\":\"PAID\",\"paidAmount\":3500}";
        String response = mockMvc.perform(post("/api/bills").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        assertThat(created.get("id").asLong()).isNotEqualTo(billA.getId());
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(created.get("paidAmount").decimalValue()).isEqualByComparingTo("0");
        MaintenanceBill original = billRepository.findById(billA.getId()).orElseThrow();
        assertThat(original.getBillingMonth()).isEqualTo("2026-04");
        assertThat(original.getStatus()).isEqualTo(BillStatus.PENDING);
    }

    @Test
    @DisplayName("Bills reject zero/negative amounts and negative late fees")
    void billAmountsValidated() throws Exception {
        mockMvc.perform(post("/api/bills").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room\":{\"id\":" + roomA.getId() + "},\"billingMonth\":\"2026-06\",\"amount\":-10,\"dueDate\":\"2026-06-15\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/bills/" + billA.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lateFee\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A bill with payments cannot have its amount or month changed; an unpaid one can")
    void paidBillsAreFrozen() throws Exception {
        MaintenanceBill partly = bill(roomA, "2026-05", "3500.00", BillStatus.PARTIALLY_PAID, "1000.00");

        mockMvc.perform(put("/api/bills/" + partly.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500}"))
                .andExpect(status().isBadRequest());
        assertThat(billRepository.findById(partly.getId()).orElseThrow().getAmount()).isEqualByComparingTo("3500.00");

        // Due date stays editable
        mockMvc.perform(put("/api/bills/" + partly.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dueDate\":\"2026-05-30\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/bills/" + billA.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":3600}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Editing only the due date keeps the bill's late fee (it used to be silently reset to 0)")
    void dueDateEditKeepsLateFee() throws Exception {
        billA.setLateFee(new BigDecimal("200.00"));
        billRepository.save(billA);

        mockMvc.perform(put("/api/bills/" + billA.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dueDate\":\"2026-04-30\"}"))
                .andExpect(status().isOk());

        MaintenanceBill updated = billRepository.findById(billA.getId()).orElseThrow();
        assertThat(updated.getLateFee()).isEqualByComparingTo("200.00");
        assertThat(updated.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("Moving a bill to a month that already has the same charge for that room is rejected")
    void billMonthChangeRejectsDuplicates() throws Exception {
        MaintenanceBill may = bill(roomA, "2026-05", "3500.00", BillStatus.PENDING, "0.00");
        mockMvc.perform(put("/api/bills/" + may.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"billingMonth\":\"2026-04\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- Resident payment submissions ----------

    @Test
    @DisplayName("A resident cannot select another flat's bill when submitting a payment")
    void submissionRejectsOtherRoomsBills() throws Exception {
        String body = "{\"roomId\":" + roomA.getId() + ",\"amount\":3500,\"paymentMode\":\"UPI_MANUAL\","
                + "\"referenceNumber\":\"UPI-X1\",\"selectedBillIds\":[" + billB.getId() + "]}";
        mockMvc.perform(post("/api/payments/submissions").header("Authorization", bearer(residentA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(submissionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Unknown payment modes, negative amounts and already-settled bills are rejected at submission")
    void submissionValidation() throws Exception {
        mockMvc.perform(post("/api/payments/submissions").header("Authorization", bearer(residentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + roomA.getId() + ",\"amount\":3500,\"paymentMode\":\"BITCOIN\",\"referenceNumber\":\"R1\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/payments/submissions").header("Authorization", bearer(residentA))
                        .param("roomId", String.valueOf(roomA.getId())).param("amount", "-50").param("paymentMode", "UPI_MANUAL"))
                .andExpect(status().isBadRequest());

        MaintenanceBill settled = bill(roomA, "2026-03", "3500.00", BillStatus.PAID, "3500.00");
        mockMvc.perform(post("/api/payments/submissions").header("Authorization", bearer(residentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + roomA.getId() + ",\"amount\":3500,\"paymentMode\":\"UPI_MANUAL\","
                                + "\"referenceNumber\":\"R2\",\"selectedBillIds\":[" + settled.getId() + "]}"))
                .andExpect(status().isBadRequest());
        assertThat(submissionRepository.count()).isZero();
    }

    private ResidentPaymentSubmission pendingSubmission(String billIdsJson) {
        ResidentPaymentSubmission sub = new ResidentPaymentSubmission();
        sub.setRoom(roomA);
        sub.setSubmittedByUser(residentA);
        sub.setAmount(new BigDecimal("3500.00"));
        sub.setPaymentMode("UPI_MANUAL");
        sub.setReferenceNumber("UPI-APPROVAL-" + System.nanoTime());
        sub.setStatus("PENDING");
        sub.setAdvanceMonthsCount(0);
        sub.setSelectedBillIdsJson(billIdsJson);
        sub.setCreatedAt(LocalDateTime.now());
        return submissionRepository.save(sub);
    }

    private int approve(ResidentPaymentSubmission sub) throws Exception {
        return mockMvc.perform(post("/api/payments/submissions/" + sub.getId() + "/approve")
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankAccountId\":" + bank.getId() + "}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("Approval re-checks the selection: a stored submission pointing at another flat's bill is refused")
    void approvalRefusesOtherRoomsBills() throws Exception {
        ResidentPaymentSubmission sub = pendingSubmission("[" + billB.getId() + "]");

        assertThat(approve(sub)).isEqualTo(400);
        assertThat(billRepository.findById(billB.getId()).orElseThrow().getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(transactionRepository.count()).isZero();
        assertThat(submissionRepository.findById(sub.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("An unreadable bill selection fails the approval instead of settling arbitrary pending bills")
    void corruptSelectionFailsLoudly() throws Exception {
        ResidentPaymentSubmission sub = pendingSubmission("not-json");

        assertThat(approve(sub)).isEqualTo(400);
        assertThat(billRepository.findById(billA.getId()).orElseThrow().getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("A valid selection is still approved and settles exactly the chosen bill")
    void validApprovalStillWorks() throws Exception {
        ResidentPaymentSubmission sub = pendingSubmission("[" + billA.getId() + "]");

        assertThat(approve(sub)).isEqualTo(200);
        assertThat(billRepository.findById(billA.getId()).orElseThrow().getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(billRepository.findById(billB.getId()).orElseThrow().getStatus()).isEqualTo(BillStatus.PENDING);
    }

    // ---------- Admin dashboard ----------

    private void credit(String amount, LocalDate date, String status) {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal(amount));
        tx.setPaymentDate(date);
        tx.setStatus(status);
        tx.setEntryDirection("CREDIT");
        tx.setPaymentMode("UPI_MANUAL");
        tx.setBankAccount(bank);
        transactionRepository.save(tx);
    }

    @Test
    @DisplayName("Dashboard figures count real money and real residents")
    void dashboardFiguresAreCorrect() throws Exception {
        YearMonth now = YearMonth.now();
        credit("3500.00", now.atDay(1), "SUCCESS");
        credit("1000.00", now.atDay(1), "REVERSED");          // reversed: not collected
        credit("2000.00", now.minusMonths(1).atDay(5), "SUCCESS");

        bill(roomA, now.toString(), "3500.00", BillStatus.PAID_CASH, "3500.00");          // fully paid in cash
        bill(roomB, now.toString(), "3500.00", BillStatus.PARTIALLY_PAID, "1500.00");     // 2000 still owed
        // billA and billB (2026-04, pending 3500 each) are also outstanding

        JsonNode stats = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/admin").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(stats.get("totalResidents").asInt()).isEqualTo(2);                    // inactive + admin excluded
        assertThat(stats.get("monthlyCollection").decimalValue()).isEqualByComparingTo("3500.00");
        assertThat(stats.get("lastMonthCollection").decimalValue()).isEqualByComparingTo("2000.00");
        assertThat(stats.get("pendingAmount").decimalValue()).isEqualByComparingTo("9000.00"); // 3500 + 3500 + 2000
        assertThat(stats.get("unpaidResidents").asInt()).isEqualTo(2);
        assertThat(stats.get("collectionTrend")).hasSize(6);
        assertThat(stats.get("collectionTrend").get(5).get("amount").decimalValue()).isEqualByComparingTo("3500.00");
        assertThat(stats.get("billStatusThisMonth").get("PAID").asInt()).isEqualTo(1);
        assertThat(stats.get("billStatusThisMonth").get("PARTIALLY_PAID").asInt()).isEqualTo(1);
    }
}
