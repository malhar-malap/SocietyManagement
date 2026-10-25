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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationEventsTests {

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
    private User masterAdmin;
    private User resident;
    private Room room;
    private SocietyBankAccount bankAccount;
    private MaintenanceBill bill;

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

        admin = user("notif-admin@example.test", Role.ADMIN, true);
        masterAdmin = user("notif-master@example.test", Role.MASTER_ADMIN, true);
        resident = user("notif-resident@example.test", Role.RESIDENT, true);

        room = room("A-101", resident);
        bankAccount = bankAccountRepository.save(new SocietyBankAccount("Society Main Account", "SBI", "1234567890", "SBIN0001234",
                SocietyBankAccountType.BANK, new BigDecimal("10000.00")));
        bill = bill(room, "2026-05", "2500.00", BillStatus.PENDING);
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

    private Room room(String number, User res) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setWing(number.substring(0, 1));
        r.setFloor(1);
        r.setResident(res);
        r = roomRepository.save(r);

        RoomResident rr = new RoomResident();
        rr.setRoom(r);
        rr.setResident(res);
        rr.setRelationshipType(RelationshipType.OWNER);
        rr.setActive(true);
        rr.setStartDate(LocalDate.now().minusYears(1));
        rr.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr);
        return r;
    }

    private MaintenanceBill bill(Room r, String month, String amount, BillStatus status) {
        MaintenanceBill b = new MaintenanceBill();
        b.setRoom(r);
        b.setBillingMonth(month);
        b.setAmount(new BigDecimal(amount));
        b.setLateFee(BigDecimal.ZERO);
        b.setPaidAmount(BigDecimal.ZERO);
        b.setDueDate(LocalDate.of(2026, 5, 10));
        b.setStatus(status);
        b.setChargeType(ChargeType.MAINTENANCE);
        return billRepository.save(b);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    @Test
    @DisplayName("Resident submits a payment proof -> active admins receive New payment proof notification")
    void residentSubmitsPaymentProofNotifiesAdmins() throws Exception {
        String submitJson = """
                {
                    "roomId": %d,
                    "amount": 2500.00,
                    "paymentMode": "UPI_MANUAL",
                    "referenceNumber": "UPI-REF-001",
                    "selectedBillIds": [%d]
                }
                """.formatted(room.getId(), bill.getId());

        mockMvc.perform(post("/api/payments/submissions")
                        .header("Authorization", bearer(resident))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isCreated());

        List<SystemNotification> adminNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId());
        assertThat(adminNotifs).hasSize(1);
        SystemNotification adminNotif = adminNotifs.get(0);
        assertThat(adminNotif.getTitle()).isEqualTo("New payment proof");
        assertThat(adminNotif.getMessage()).isEqualTo("A-101 submitted ₹2500.00 (UPI_MANUAL) for review.");
        assertThat(adminNotif.getLink()).isEqualTo("/admin/billing");

        List<SystemNotification> masterNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(masterAdmin.getId());
        assertThat(masterNotifs).hasSize(1);
        assertThat(masterNotifs.get(0).getTitle()).isEqualTo("New payment proof");
    }

    @Test
    @DisplayName("Admin approves a payment submission -> submitter receives Payment approved notification")
    void adminApprovesSubmissionNotifiesSubmitter() throws Exception {
        String submitJson = """
                {
                    "roomId": %d,
                    "amount": 2500.00,
                    "paymentMode": "UPI_MANUAL",
                    "referenceNumber": "UPI-REF-002",
                    "selectedBillIds": [%d]
                }
                """.formatted(room.getId(), bill.getId());

        String resContent = mockMvc.perform(post("/api/payments/submissions")
                        .header("Authorization", bearer(resident))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(resContent);
        long submissionId = jsonNode.get("id").asLong();

        String approveJson = """
                {
                    "bankAccountId": %d
                }
                """.formatted(bankAccount.getId());

        mockMvc.perform(post("/api/payments/submissions/" + submissionId + "/approve")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveJson))
                .andExpect(status().isOk());

        List<SystemNotification> residentNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(resident.getId());
        assertThat(residentNotifs).hasSize(1);
        SystemNotification notif = residentNotifs.get(0);
        assertThat(notif.getTitle()).isEqualTo("Payment approved");
        assertThat(notif.getMessage()).contains("Your payment of ₹2500.00 for room A-101 was approved.");
        assertThat(notif.getLink()).isEqualTo("/resident/invoices");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Payment approved"))
                .andExpect(jsonPath("$[0].message").value(notif.getMessage()));
    }

    @Test
    @DisplayName("Admin rejects a submission -> submitter receives Payment proof rejected notification with reason")
    void adminRejectsSubmissionNotifiesSubmitterWithReason() throws Exception {
        String submitJson = """
                {
                    "roomId": %d,
                    "amount": 2500.00,
                    "paymentMode": "UPI_MANUAL",
                    "referenceNumber": "UPI-REF-003",
                    "selectedBillIds": [%d]
                }
                """.formatted(room.getId(), bill.getId());

        String resContent = mockMvc.perform(post("/api/payments/submissions")
                        .header("Authorization", bearer(resident))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(resContent);
        long submissionId = jsonNode.get("id").asLong();

        String rejectJson = """
                {
                    "reason": "Blurry screenshot"
                }
                """;

        mockMvc.perform(post("/api/payments/submissions/" + submissionId + "/reject")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectJson))
                .andExpect(status().isOk());

        List<SystemNotification> residentNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(resident.getId());
        assertThat(residentNotifs).hasSize(1);
        SystemNotification notif = residentNotifs.get(0);
        assertThat(notif.getTitle()).isEqualTo("Payment proof rejected");
        assertThat(notif.getMessage()).contains("Blurry screenshot");
        assertThat(notif.getLink()).isEqualTo("/resident/invoices");
    }

    @Test
    @DisplayName("Admin responds to a complaint -> resident gets Update on your complaint with status and response")
    void adminRespondsToComplaintNotifiesResident() throws Exception {
        Complaint complaint = new Complaint(resident, "PLUMBING", "Leakage in bathroom pipe");
        complaint.setSubmissionType("COMPLAINT");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint = complaintRepository.save(complaint);

        String respondJson = """
                {
                    "status": "RESOLVED",
                    "adminResponse": "Fixed"
                }
                """;

        mockMvc.perform(post("/api/complaints/" + complaint.getId() + "/respond")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondJson))
                .andExpect(status().isOk());

        List<SystemNotification> residentNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(resident.getId());
        assertThat(residentNotifs).hasSize(1);
        SystemNotification notif = residentNotifs.get(0);
        assertThat(notif.getTitle()).isEqualTo("Update on your complaint");
        assertThat(notif.getMessage()).contains("RESOLVED");
        assertThat(notif.getMessage()).contains("Fixed");
        assertThat(notif.getLink()).isEqualTo("/resident/complaints");
    }

    @Test
    @DisplayName("Master Admin posts an announcement -> broadcast notification is returned for resident with broadcast=true")
    void masterAdminPostsAnnouncementBroadcastsNotification() throws Exception {
        String announcementJson = """
                {
                    "title": "Water Tank Cleaning Tomorrow",
                    "description": "Water supply will be paused from 10am to 2pm."
                }
                """;

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", bearer(masterAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson))
                .andExpect(status().isOk());

        List<SystemNotification> broadcasts = notificationRepository.findByUserIdIsNullOrderByCreatedAtDesc();
        assertThat(broadcasts).hasSize(1);
        SystemNotification broadcast = broadcasts.get(0);
        assertThat(broadcast.getTitle()).isEqualTo("New announcement");
        assertThat(broadcast.getMessage()).isEqualTo("Water Tank Cleaning Tomorrow");
        assertThat(broadcast.getLink()).isNull();

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("New announcement"))
                .andExpect(jsonPath("$[0].broadcast").value(true));
    }
}
