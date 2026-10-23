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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileSetupEmailVerificationTests {

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

    private User residentUser;
    private User otherUser;
    private String residentToken;

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

        // 1. Existing resident with forcePasswordChange = true
        residentUser = new User();
        residentUser.setName("Resident 602");
        residentUser.setEmail("resident602@jaswanti.com");
        residentUser.setPassword("$2a$10$hashedpassword");
        residentUser.setRole(Role.RESIDENT);
        residentUser.setStatus(UserStatus.ACTIVE);
        residentUser.setActive(true);
        residentUser.setForcePasswordChange(true);
        residentUser = userRepository.save(residentUser);

        // 2. Another user
        otherUser = new User();
        otherUser.setName("Existing User");
        otherUser.setEmail("taken@example.com");
        otherUser.setPassword("$2a$10$hashedpassword");
        otherUser.setRole(Role.RESIDENT);
        otherUser.setStatus(UserStatus.ACTIVE);
        otherUser.setActive(true);
        userRepository.save(otherUser);

        // 3. Room
        Room room = new Room();
        room.setRoomNumber("C-602");
        room.setFloor(6);
        room.setWing("C");
        room.setResident(residentUser);
        roomRepository.save(room);

        residentToken = jwtService.generateToken(residentUser);
    }

    @Test
    @DisplayName("Initiating profile setup stores pending email, creates 4-digit token, and sends verification")
    void testSetupProfileSuccess() throws Exception {
        String setupPayload = """
                {
                    "name": "Malhar Resident",
                    "phone": "9876543210",
                    "personalEmail": "newresident602@example.com",
                    "newPassword": "NewStrongPassword123"
                }
                """;

        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setupPayload)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Profile setup initiated")));

        User updated = userRepository.findById(residentUser.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Malhar Resident");
        assertThat(updated.getPendingEmail()).isEqualTo("newresident602@example.com");
        assertThat(updated.getEmailVerificationToken()).isNotNull().hasSize(4);
        assertThat(updated.getEmailVerificationExpiry()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Initiating setup with an already registered email is rejected with 400 Bad Request")
    void testSetupDuplicateEmailRejected() throws Exception {
        String setupPayload = """
                {
                    "name": "Duplicate Tester",
                    "phone": "9876543210",
                    "personalEmail": "taken@example.com",
                    "newPassword": "NewStrongPassword123"
                }
                """;

        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setupPayload)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("already in use")));
    }

    @Test
    @DisplayName("Verifying setup email with valid OTP (including spaces) succeeds and returns fresh JWT AuthResponse")
    void testVerifySetupEmailWithWhitespace() throws Exception {
        // Prepare pending setup state
        residentUser.setPendingEmail("verified.resident@example.com");
        residentUser.setEmailVerificationToken("7336");
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(residentUser);

        // Send OTP with trailing/leading spaces: "  7336 "
        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "  7336 ")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.forcePasswordChange", is(false)))
                .andExpect(jsonPath("$.userId", is(residentUser.getId().intValue())));

        User verified = userRepository.findById(residentUser.getId()).orElseThrow();
        assertThat(verified.getEmail()).isEqualTo("verified.resident@example.com");
        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(verified.isForcePasswordChange()).isFalse();
        assertThat(verified.getPendingEmail()).isNull();
        assertThat(verified.getEmailVerificationToken()).isNull();
        assertThat(verified.getEmailVerificationExpiry()).isNull();
    }

    @Test
    @DisplayName("Verifying setup email with incorrect or expired OTP returns 400 Bad Request")
    void testVerifySetupEmailIncorrectOrExpiredToken() throws Exception {
        residentUser.setPendingEmail("verified.resident@example.com");
        residentUser.setEmailVerificationToken("7336");
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(residentUser);

        // Incorrect token
        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "9999")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token."));

        // Expired token
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().minusHours(1));
        userRepository.save(residentUser);

        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "7336")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token."));
    }
}
