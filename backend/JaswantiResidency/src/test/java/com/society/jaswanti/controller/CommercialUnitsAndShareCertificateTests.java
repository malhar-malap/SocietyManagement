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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommercialUnitsAndShareCertificateTests {

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
    private TransactionRepository transactionRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private JwtService jwtService;

    private User masterAdmin;
    private User admin;
    private User resident1;
    private User resident2;
    private Room resRoom;
    private Room commRoom;

    private String masterAdminToken;
    private String adminToken;
    private String resident1Token;
    private String resident2Token;

    @BeforeEach
    void setUp() {
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
        masterAdmin.setName("Main Master Admin");
        masterAdmin.setEmail("master.phase2b@example.com");
        masterAdmin.setPassword("$2a$10$hashedpassword");
        masterAdmin.setRole(Role.MASTER_ADMIN);
        masterAdmin.setStatus(UserStatus.ACTIVE);
        masterAdmin.setActive(true);
        masterAdmin = userRepository.save(masterAdmin);

        admin = new User();
        admin.setName("Regular Admin");
        admin.setEmail("admin.phase2b@example.com");
        admin.setPassword("$2a$10$hashedpassword");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setActive(true);
        admin = userRepository.save(admin);

        resident1 = new User();
        resident1.setName("Resident One");
        resident1.setEmail("res1.phase2b@example.com");
        resident1.setPassword("$2a$10$hashedpassword");
        resident1.setRole(Role.RESIDENT);
        resident1.setStatus(UserStatus.ACTIVE);
        resident1.setActive(true);
        resident1 = userRepository.save(resident1);

        resident2 = new User();
        resident2.setName("Resident Two");
        resident2.setEmail("res2.phase2b@example.com");
        resident2.setPassword("$2a$10$hashedpassword");
        resident2.setRole(Role.RESIDENT);
        resident2.setStatus(UserStatus.ACTIVE);
        resident2.setActive(true);
        resident2 = userRepository.save(resident2);

        // 2. Rooms
        resRoom = new Room();
        resRoom.setRoomNumber("101");
        resRoom.setFloor(1);
        resRoom.setWing("A");
        resRoom.setUnitCategory("RESIDENTIAL");
        resRoom.setNocApplicable(false);
        resRoom.setNocAmount(BigDecimal.ZERO);
        resRoom.setResident(resident1);
        resRoom = roomRepository.save(resRoom);

        commRoom = new Room();
        commRoom.setRoomNumber("G-01");
        commRoom.setFloor(0);
        commRoom.setWing("G");
        commRoom.setUnitCategory("COMMERCIAL");
        commRoom.setNocApplicable(true);
        commRoom.setNocAmount(new BigDecimal("750.00"));
        commRoom.setResident(resident2);
        commRoom = roomRepository.save(commRoom);

        // 3. Link RoomResidents
        RoomResident rr1 = new RoomResident();
        rr1.setRoom(resRoom);
        rr1.setResident(resident1);
        rr1.setRelationshipType(RelationshipType.OWNER);
        rr1.setActive(true);
        rr1.setStartDate(LocalDate.now());
        rr1.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr1);

        RoomResident rr2 = new RoomResident();
        rr2.setRoom(commRoom);
        rr2.setResident(resident2);
        rr2.setRelationshipType(RelationshipType.OWNER);
        rr2.setActive(true);
        rr2.setStartDate(LocalDate.now());
        rr2.setCreatedAt(LocalDateTime.now());
        roomResidentRepository.save(rr2);

        masterAdminToken = jwtService.generateToken(masterAdmin);
        adminToken = jwtService.generateToken(admin);
        resident1Token = jwtService.generateToken(resident1);
        resident2Token = jwtService.generateToken(resident2);
    }

    @Test
    @DisplayName("Batch bill generation creates automatic companion NOC bill for commercial unit")
    void batchGenerationCreatesCompanionNocBill() throws Exception {
        mockMvc.perform(post("/api/bills/generate-monthly")
                        .param("billingMonth", "2026-06")
                        .param("amount", "3000.00")
                        .param("dueDate", "2026-06-15")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Residential room (101): should have 1 bill (MAINTENANCE = 3000.00)
        var resBills = billRepository.findByRoomId(resRoom.getId());
        assertThat(resBills).hasSize(1);
        assertThat(resBills.get(0).getChargeType()).isEqualTo(ChargeType.MAINTENANCE);
        assertThat(resBills.get(0).getAmount()).isEqualByComparingTo("3000.00");

        // Commercial room (G-01): should have 2 bills (MAINTENANCE = 3000.00, NOC = 750.00)
        var commBills = billRepository.findByRoomId(commRoom.getId());
        assertThat(commBills).hasSize(2);
        assertThat(commBills).anyMatch(b -> b.getChargeType() == ChargeType.MAINTENANCE && b.getAmount().compareTo(new BigDecimal("3000.00")) == 0);
        assertThat(commBills).anyMatch(b -> b.getChargeType() == ChargeType.NOC && b.getAmount().compareTo(new BigDecimal("750.00")) == 0);
    }

    @Test
    @DisplayName("Master Admin can upload Share Certificate with document file")
    void masterAdminCanUploadShareCertificate() throws Exception {
        MockMultipartFile certFile = new MockMultipartFile(
                "file",
                "cert_101.pdf",
                "application/pdf",
                "%PDF-1.4 sample share certificate content".getBytes()
        );

        mockMvc.perform(multipart("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .file(certFile)
                        .param("certificateNumber", "SC-JR-101")
                        .param("issuedDate", "2026-01-10")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCertificateNumber").value("SC-JR-101"))
                .andExpect(jsonPath("$.shareCertificateIssuedDate").value("2026-01-10"))
                .andExpect(jsonPath("$.hasDocument").value(true));

        Room updated = roomRepository.findById(resRoom.getId()).orElseThrow();
        assertThat(updated.getShareCertificateNumber()).isEqualTo("SC-JR-101");
        assertThat(updated.getShareCertificateData()).isNotEmpty();
    }

    @Test
    @DisplayName("Regular Admin is forbidden from uploading share certificate (Master Admin only)")
    void regularAdminForbiddenFromUploadingShareCert() throws Exception {
        MockMultipartFile certFile = new MockMultipartFile(
                "file",
                "cert_101.pdf",
                "application/pdf",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .file(certFile)
                        .param("certificateNumber", "SC-JR-FAIL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Linked resident can view and download their room's share certificate")
    void linkedResidentCanDownloadShareCert() throws Exception {
        // Master Admin uploads
        MockMultipartFile certFile = new MockMultipartFile(
                "file",
                "cert_resRoom.pdf",
                "application/pdf",
                "%PDF-1.4 PDF_SECRET_CERTIFICATE_BYTES".getBytes()
        );

        mockMvc.perform(multipart("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .file(certFile)
                        .param("certificateNumber", "SC-RES1")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk());

        // Resident 1 (linked) views
        mockMvc.perform(get("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCertificateNumber").value("SC-RES1"));

        // Resident 1 (linked) downloads
        mockMvc.perform(get("/api/rooms/" + resRoom.getId() + "/share-certificate/download")
                        .header("Authorization", "Bearer " + resident1Token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("%PDF-1.4 PDF_SECRET_CERTIFICATE_BYTES".getBytes()));
    }

    @Test
    @DisplayName("Unlinked resident is forbidden from downloading another room's share certificate")
    void unlinkedResidentForbiddenFromDownloadingShareCert() throws Exception {
        // Master Admin uploads for resRoom (101)
        MockMultipartFile certFile = new MockMultipartFile(
                "file",
                "cert.pdf",
                "application/pdf",
                "%PDF-1.4 PDF_SECRET".getBytes()
        );

        mockMvc.perform(multipart("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .file(certFile)
                        .param("certificateNumber", "SC-SECURE")
                        .header("Authorization", "Bearer " + masterAdminToken))
                .andExpect(status().isOk());

        // Resident 2 (occupant of G-01) tries to access resRoom (101)
        mockMvc.perform(get("/api/rooms/" + resRoom.getId() + "/share-certificate")
                        .header("Authorization", "Bearer " + resident2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/rooms/" + resRoom.getId() + "/share-certificate/download")
                        .header("Authorization", "Bearer " + resident2Token))
                .andExpect(status().isForbidden());
    }
}
