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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileSetupEmailVerificationTests {

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
    @DisplayName("Initiating profile setup stores pending email, a hashed 6-digit code valid for 15 minutes, and sends verification")
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
        // Only a BCrypt hash of the code is stored, never the code itself
        assertThat(updated.getEmailVerificationToken()).isNotNull().startsWith("$2");
        assertThat(updated.getEmailVerificationExpiry()).isAfter(LocalDateTime.now())
                .isBefore(LocalDateTime.now().plusMinutes(16));
        assertThat(updated.getEmailVerificationAttempts()).isZero();
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
        residentUser.setEmailVerificationToken(passwordEncoder.encode("733612"));
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(residentUser);

        // Send OTP with trailing/leading spaces: "  733612 "
        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "  733612 ")
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
        residentUser.setEmailVerificationToken(passwordEncoder.encode("733612"));
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(residentUser);

        // Incorrect token
        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "999999")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token."));

        // Expired token
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().minusHours(1));
        userRepository.save(residentUser);

        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "733612")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token."));
    }

    @Test
    @DisplayName("Resident can update own profile name and phone via PUT /api/users/profile/{id}")
    void testResidentCanUpdateOwnProfileNameAndPhone() throws Exception {
        String updatePayload = """
                {
                    "name": "Jane Resident",
                    "phone": "9876500000"
                }
                """;

        mockMvc.perform(put("/api/users/profile/" + residentUser.getId())
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Jane Resident")))
                .andExpect(jsonPath("$.phoneNumber", is("9876500000")));

        User updated = userRepository.findById(residentUser.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Jane Resident");
        assertThat(updated.getPhone()).isEqualTo("9876500000");
    }

    @Test
    @DisplayName("Another resident cannot update someone else's profile (403 Forbidden)")
    void testOtherResidentCannotUpdateSomeoneElsesProfile() throws Exception {
        User otherResident = new User();
        otherResident.setEmail("other.resident@jaswanti.com");
        otherResident.setName("Other Resident");
        otherResident.setPassword("Pass123!");
        otherResident.setRole(Role.RESIDENT);
        otherResident.setStatus(UserStatus.ACTIVE);
        otherResident.setActive(true);
        otherResident = userRepository.save(otherResident);
        String otherToken = jwtService.generateToken(otherResident);

        String updatePayload = """
                {
                    "name": "Hacked Name",
                    "phone": "1234567890"
                }
                """;

        mockMvc.perform(put("/api/users/profile/" + residentUser.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An access token in the URL (?token=) is rejected: tokens are only accepted in the Authorization header")
    void testJwtTokenInQueryParamIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/profile/" + residentUser.getId())
                        .param("token", residentToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("After 5 wrong setup codes the code is locked, even if the right code is then entered")
    void testSetupCodeLocksAfterFiveWrongAttempts() throws Exception {
        residentUser.setPendingEmail("locked.resident@example.com");
        residentUser.setEmailVerificationToken(passwordEncoder.encode("246810"));
        residentUser.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        residentUser.setEmailVerificationAttempts(0);
        userRepository.save(residentUser);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                            .param("token", "11111" + i)
                            .header("Authorization", "Bearer " + residentToken))
                    .andExpect(status().isBadRequest());
        }
        assertThat(userRepository.findById(residentUser.getId()).orElseThrow().getEmailVerificationAttempts()).isEqualTo(5);

        mockMvc.perform(post("/api/users/" + residentUser.getId() + "/verify-setup")
                        .param("token", "246810")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Too many incorrect codes")));
        assertThat(userRepository.findById(residentUser.getId()).orElseThrow().isEmailVerified()).isFalse();
    }
}
