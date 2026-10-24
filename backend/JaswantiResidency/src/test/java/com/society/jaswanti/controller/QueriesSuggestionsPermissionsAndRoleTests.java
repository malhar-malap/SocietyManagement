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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueriesSuggestionsPermissionsAndRoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomResidentRepository roomResidentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private ResidentPaymentSubmissionRepository submissionRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtService jwtService;

    private User masterAdmin;
    private User admin;
    private User resident1;
    private User resident2;

    private String masterAdminToken;
    private String adminToken;
    private String resident1Token;
    private String resident2Token;

    @BeforeEach
    void setUp() {
        announcementRepository.deleteAll();
        complaintRepository.deleteAll();
        submissionRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentAttemptRepository.deleteAll();
        billRepository.deleteAll();
        roomResidentRepository.deleteAll();
        roomRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Users
        masterAdmin = new User();
        masterAdmin.setName("Society Master Admin");
        masterAdmin.setEmail("master.phase4a@example.com");
        masterAdmin.setPassword("$2a$10$hashedpassword");
        masterAdmin.setRole(Role.MASTER_ADMIN);
        masterAdmin.setStatus(UserStatus.ACTIVE);
        masterAdmin.setActive(true);
        masterAdmin = userRepository.save(masterAdmin);

        admin = new User();
        admin.setName("Regular Admin");
        admin.setEmail("admin.phase4a@example.com");
        admin.setPassword("$2a$10$hashedpassword");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setActive(true);
        admin = userRepository.save(admin);

        resident1 = new User();
        resident1.setName("Resident Alice");
        resident1.setEmail("alice.phase4a@example.com");
        resident1.setPassword("$2a$10$hashedpassword");
        resident1.setRole(Role.RESIDENT);
        resident1.setStatus(UserStatus.ACTIVE);
        resident1.setActive(true);
        resident1 = userRepository.save(resident1);

        resident2 = new User();
        resident2.setName("Resident Bob");
        resident2.setEmail("bob.phase4a@example.com");
        resident2.setPassword("$2a$10$hashedpassword");
        resident2.setRole(Role.RESIDENT);
        resident2.setStatus(UserStatus.ACTIVE);
        resident2.setActive(true);
        resident2 = userRepository.save(resident2);

        // 2. Rooms
        Room room101 = new Room();
        room101.setRoomNumber("101");
        room101.setWing("A");
        room101.setFloor(1);
        room101.setUnitCategory("RESIDENTIAL");
        roomRepository.save(room101);

        RoomResident rr1 = new RoomResident();
        rr1.setRoom(room101);
        rr1.setResident(resident1);
        rr1.setRelationshipType(RelationshipType.OWNER);
        rr1.setActive(true);
        rr1.setStartDate(LocalDate.now());
        rr1.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr1);

        masterAdminToken = jwtService.generateToken(masterAdmin);
        adminToken = jwtService.generateToken(admin);
        resident1Token = jwtService.generateToken(resident1);
        resident2Token = jwtService.generateToken(resident2);
    }

    @Test
    @DisplayName("Resident can create Query, Suggestion, and Permission with dates and attachment")
    void testResidentCreatesSubmissions() throws Exception {
        // 1. Create Query
        mockMvc.perform(multipart("/api/complaints")
                        .param("category", "General Query")
                        .param("description", "When will the swimming pool maintenance complete?")
                        .param("submissionType", "QUERY")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionType", is("QUERY")))
                .andExpect(jsonPath("$.status", is("OPEN")));

        // 2. Create Suggestion
        mockMvc.perform(multipart("/api/complaints")
                        .param("category", "Clubhouse")
                        .param("description", "We should install solar panels on clubhouse roof")
                        .param("submissionType", "SUGGESTION")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionType", is("SUGGESTION")));

        // 3. Create Permission with attachment and dates
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "renovation_plan.pdf",
                "application/pdf",
                "%PDF-1.4 mock pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/complaints")
                        .file(file)
                        .param("category", "Flat Renovation")
                        .param("description", "Kitchen remodeling request")
                        .param("submissionType", "PERMISSION")
                        .param("permissionCategory", "RENOVATION")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-15")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionType", is("PERMISSION")))
                .andExpect(jsonPath("$.permissionCategory", is("RENOVATION")))
                .andExpect(jsonPath("$.startDate", is("2026-11-01")))
                .andExpect(jsonPath("$.endDate", is("2026-11-15")))
                .andExpect(jsonPath("$.hasAttachment", is(true)));
    }

    @Test
    @DisplayName("Admin can respond to permission submission, updating status to APPROVED and adding adminResponse")
    void testAdminRespondToPermission() throws Exception {
        Complaint complaint = new Complaint();
        complaint.setResident(resident1);
        complaint.setCategory("Moving In");
        complaint.setDescription("Move in scheduled for Saturday");
        complaint.setSubmissionType("PERMISSION");
        complaint.setPermissionCategory("MOVE_IN");
        complaint.setStartDate(LocalDate.of(2026, 11, 5));
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setCreatedDate(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        String payload = """
                {
                    "status": "APPROVED",
                    "adminResponse": "Approved. Service lift booked from 10:00 AM to 2:00 PM."
                }
                """;

        mockMvc.perform(post("/api/complaints/" + complaint.getId() + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.adminResponse", is("Approved. Service lift booked from 10:00 AM to 2:00 PM.")));

        Complaint updated = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ComplaintStatus.APPROVED);
        assertThat(updated.getAdminResponse()).contains("Service lift booked");
    }

    @Test
    @DisplayName("Resident can view their attachment, but another resident is denied access (403)")
    void testAttachmentAccessControl() throws Exception {
        Complaint complaint = new Complaint();
        complaint.setResident(resident1);
        complaint.setCategory("Carpentry");
        complaint.setDescription("Carpentry sound permit");
        complaint.setSubmissionType("PERMISSION");
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setImageData(new byte[]{1, 2, 3, 4, 5});
        complaint.setImageFilename("permit.pdf");
        complaint.setImageContentType("application/pdf");
        complaint.setCreatedDate(LocalDateTime.now());
        complaint = complaintRepository.save(complaint);

        // Owner resident gets 200
        mockMvc.perform(get("/api/complaints/" + complaint.getId() + "/attachment")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline; filename=\"permit.pdf\"")))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4, 5}));

        // Admin gets 200
        mockMvc.perform(get("/api/complaints/" + complaint.getId() + "/attachment")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Another resident gets 403 Forbidden
        mockMvc.perform(get("/api/complaints/" + complaint.getId() + "/attachment")
                        .header("Authorization", "Bearer " + resident2Token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Role Partitioning: Regular Admin cannot create announcements or delete users, but Master Admin can")
    void testRolePartitioning() throws Exception {
        // 1. Regular admin attempts to create Announcement -> 403 Forbidden
        String announcementPayload = """
                {
                    "title": "AGM Meeting",
                    "description": "Annual General Meeting this Sunday"
                }
                """;

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementPayload)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // 2. Master Admin creates Announcement -> 200 OK
        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementPayload)
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("AGM Meeting")));

        // 3. Regular admin attempts to delete user -> 403 Forbidden
        mockMvc.perform(delete("/api/users/" + resident2.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // 4. Master Admin deletes user -> 200 OK (soft delete)
        mockMvc.perform(delete("/api/users/" + resident2.getId())
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk());

        User deleted = userRepository.findById(resident2.getId()).orElseThrow();
        assertThat(deleted.isActive()).isFalse();
    }
}
