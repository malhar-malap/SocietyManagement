package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.dto.AuthRequest;
import com.society.jaswanti.dto.ForgotPasswordRequest;
import com.society.jaswanti.dto.ResetPasswordRequest;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.EmailService;
import com.society.jaswanti.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PasswordRecoveryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private DataSource dataSource;

    @MockitoSpyBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Prevent real mail delivery while allowing spy verification
        doNothing().when(emailService).sendEmail(any(), any(), any());
        passwordResetService.clearCooldownCache();
    }

    private User createOrUpdateTestUser(String email, String plainPassword) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName("Test " + email);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(Role.RESIDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        user.setForcePasswordChange(false);
        user.setPasswordResetOtpHash(null);
        user.setPasswordResetOtpExpiry(null);
        user.setPasswordResetOtpAttempts(0);
        user.setPasswordResetOtpLastSentAt(null);
        return userRepository.save(user);
    }

    private String extractOtpFromBody(String body) {
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(body);
        assertTrue(matcher.find(), "Expected 6-digit OTP in email body");
        return matcher.group(1);
    }

    @Test
    @DisplayName("Valid reset flow: requests OTP via JSON, resets password, and logs in with new password")
    void validReset_updatesPasswordAndEnablesLogin() throws Exception {
        String email = "recovery_user1@jaswanti.com";
        createOrUpdateTestUser(email, "oldPassword123");

        // 1. Request OTP via JSON body
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(email);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If your email is registered, you will receive a password reset OTP shortly.")));

        // 2. Verify email was sent and capture the OTP
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(toCaptor.capture(), any(), bodyCaptor.capture());
        assertEquals(email, toCaptor.getValue());

        String otp = extractOtpFromBody(bodyCaptor.getValue());
        assertEquals(6, otp.length());

        // Verify stored hash in DB matches OTP but is not stored in plaintext
        User userAfterOtp = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotNull(userAfterOtp.getPasswordResetOtpHash());
        assertNotEquals(otp, userAfterOtp.getPasswordResetOtpHash());
        assertTrue(passwordEncoder.matches(otp, userAfterOtp.getPasswordResetOtpHash()));
        assertEquals(0, userAfterOtp.getPasswordResetOtpAttempts());
        assertNotNull(userAfterOtp.getPasswordResetOtpExpiry());
        assertTrue(userAfterOtp.getPasswordResetOtpExpiry().isAfter(LocalDateTime.now()));

        // 3. Reset password using valid OTP
        ResetPasswordRequest resetReq = new ResetPasswordRequest(email, otp, "brandNewPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password reset successfully.")));

        // 4. Verify DB state: OTP is cleared/invalidated (single-use)
        User userAfterReset = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNull(userAfterReset.getPasswordResetOtpHash());
        assertNull(userAfterReset.getPasswordResetOtpExpiry());
        assertEquals(0, userAfterReset.getPasswordResetOtpAttempts());

        // 5. Old password must fail login
        AuthRequest oldLogin = new AuthRequest();
        oldLogin.setIdentifier(email);
        oldLogin.setPassword("oldPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // 6. New password must succeed login
        AuthRequest newLogin = new AuthRequest();
        newLogin.setIdentifier(email);
        newLogin.setPassword("brandNewPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role", is("RESIDENT")));
    }

    @Test
    @DisplayName("Invalid OTP is rejected and increments failed attempts counter")
    void invalidOtp_rejectedAndIncrementsFailedAttempts() throws Exception {
        String email = "recovery_user2@jaswanti.com";
        createOrUpdateTestUser(email, "secretPassword123");

        // Request OTP via JSON body
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        // Attempt reset with wrong OTP
        ResetPasswordRequest resetReq = new ResetPasswordRequest(email, "000000", "newPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Verify attempt count incremented in database
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(1, user.getPasswordResetOtpAttempts());
        assertNotNull(user.getPasswordResetOtpHash());

        // Old password still works
        assertTrue(passwordEncoder.matches("secretPassword123", user.getPassword()));
    }

    @Test
    @DisplayName("Attempt limit: OTP is invalidated and locked out after 5 failed attempts")
    void attemptLimit_locksOutOtpAfterFiveFailedAttempts() throws Exception {
        String email = "recovery_user3@jaswanti.com";
        createOrUpdateTestUser(email, "secretPassword123");

        // Request OTP via JSON body
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(eq(email), any(), bodyCaptor.capture());
        String realOtp = extractOtpFromBody(bodyCaptor.getValue());

        // Submit 5 wrong OTPs
        for (int i = 1; i <= 5; i++) {
            ResetPasswordRequest wrongReq = new ResetPasswordRequest(email, String.format("99999%d", i % 10), "newPassword123");
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));
        }

        // Verify OTP hash is now invalidated in DB
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNull(user.getPasswordResetOtpHash());
        assertNull(user.getPasswordResetOtpExpiry());

        // Now submitting the REAL OTP must still be rejected
        ResetPasswordRequest validReq = new ResetPasswordRequest(email, realOtp, "newPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));
    }

    @Test
    @DisplayName("Expired OTP is rejected and does not change password")
    void expiredOtp_rejected() throws Exception {
        String email = "recovery_user4@jaswanti.com";
        createOrUpdateTestUser(email, "originalPassword123");

        // Request OTP via JSON body
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(eq(email), any(), bodyCaptor.capture());
        String otp = extractOtpFromBody(bodyCaptor.getValue());

        // Manually expire OTP in database
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setPasswordResetOtpExpiry(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        // Attempt reset with expired OTP
        ResetPasswordRequest resetReq = new ResetPasswordRequest(email, otp, "newPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Verify password remained unchanged
        User freshUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(passwordEncoder.matches("originalPassword123", freshUser.getPassword()));
    }

    @Test
    @DisplayName("Single-use OTP: reused OTP is rejected on second attempt")
    void reusedOtp_rejectedBecauseSingleUse() throws Exception {
        String email = "recovery_user5@jaswanti.com";
        createOrUpdateTestUser(email, "initialPassword123");

        // Request OTP via JSON body
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(eq(email), any(), bodyCaptor.capture());
        String otp = extractOtpFromBody(bodyCaptor.getValue());

        // First reset succeeds
        ResetPasswordRequest firstReset = new ResetPasswordRequest(email, otp, "firstNewPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstReset)))
                .andExpect(status().isOk());

        // Second reset with the same OTP fails
        ResetPasswordRequest secondReset = new ResetPasswordRequest(email, otp, "secondNewPassword456");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Password remains firstNewPassword123
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(passwordEncoder.matches("firstNewPassword123", user.getPassword()));
    }

    @Test
    @DisplayName("Resend cooldown: request within 60 seconds returns 429; allows resend after 60 seconds")
    void resendCooldown_enforcedWithinSixtySeconds() throws Exception {
        String email = "recovery_user6@jaswanti.com";
        createOrUpdateTestUser(email, "userPassword123");

        // First request: succeeds
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If your email is registered, you will receive a password reset OTP shortly.")));

        // Immediate second request: rejected with 429
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.message", is("Please wait before requesting another OTP.")));

        // Advance cooldown by modifying lastSentAt in DB beyond 60s
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setPasswordResetOtpLastSentAt(LocalDateTime.now().minusSeconds(65));
        userRepository.save(user);

        // Third request after cooldown: succeeds
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If your email is registered, you will receive a password reset OTP shortly.")));
    }

    @Test
    @DisplayName("Resend cooldown: enforced even for unknown email to prevent account enumeration")
    void resendCooldown_enforcedEvenForUnknownEmail() throws Exception {
        String unknownEmail = "unknown_resend@jaswanti.com";

        // First request for unknown email: 200 OK
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unknownEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If your email is registered, you will receive a password reset OTP shortly.")));

        // Immediate second request for unknown email: 429 Too Many Requests
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unknownEmail))))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.message", is("Please wait before requesting another OTP.")));
    }

    @Test
    @DisplayName("Unknown email returns generic success on forgot-password without sending email")
    void unknownEmail_returnsGenericSuccessOnForgotPassword() throws Exception {
        String unknownEmail = "ghost_account@jaswanti.com";

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(unknownEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If your email is registered, you will receive a password reset OTP shortly.")));

        // Ensure email was never sent to this non-existent account
        verify(emailService, never()).sendEmail(eq(unknownEmail), any(), any());
    }

    @Test
    @DisplayName("Unknown email returns generic 400 Bad Request on reset-password")
    void unknownEmail_returnsGenericBadRequestOnResetPassword() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest("ghost_account@jaswanti.com", "123456", "validPassword123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));
    }

    @Test
    @DisplayName("Reset failures are generic across unknown accounts and accounts with missing, expired, or locked OTPs")
    void resetFailuresAreGenericAcrossAllIneligibleCases() throws Exception {
        // Case 1: Unknown account
        ResetPasswordRequest reqUnknown = new ResetPasswordRequest("unknown_timing_test@jaswanti.com", "123456", "validPass123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqUnknown)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Case 2: Known account with missing OTP (never requested)
        String noOtpUser = "known_no_otp@jaswanti.com";
        createOrUpdateTestUser(noOtpUser, "existingPass123");
        ResetPasswordRequest reqNoOtp = new ResetPasswordRequest(noOtpUser, "123456", "validPass123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNoOtp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Case 3: Known account with expired OTP
        String expiredUser = "known_expired@jaswanti.com";
        User userExp = createOrUpdateTestUser(expiredUser, "existingPass123");
        userExp.setPasswordResetOtpHash(passwordEncoder.encode("123456"));
        userExp.setPasswordResetOtpExpiry(LocalDateTime.now().minusMinutes(5));
        userExp.setPasswordResetOtpAttempts(0);
        userRepository.save(userExp);

        ResetPasswordRequest reqExpired = new ResetPasswordRequest(expiredUser, "123456", "validPass123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqExpired)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Case 4: Known account with locked OTP (>= 5 attempts)
        String lockedUser = "known_locked@jaswanti.com";
        User userLocked = createOrUpdateTestUser(lockedUser, "existingPass123");
        userLocked.setPasswordResetOtpHash(passwordEncoder.encode("123456"));
        userLocked.setPasswordResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userLocked.setPasswordResetOtpAttempts(5);
        userRepository.save(userLocked);

        ResetPasswordRequest reqLocked = new ResetPasswordRequest(lockedUser, "123456", "validPass123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLocked)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));

        // Case 5: Known account with active OTP but incorrect code
        String wrongOtpUser = "known_wrong_otp@jaswanti.com";
        User userWrong = createOrUpdateTestUser(wrongOtpUser, "existingPass123");
        userWrong.setPasswordResetOtpHash(passwordEncoder.encode("654321"));
        userWrong.setPasswordResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userWrong.setPasswordResetOtpAttempts(0);
        userRepository.save(userWrong);

        ResetPasswordRequest reqWrong = new ResetPasswordRequest(wrongOtpUser, "123456", "validPass123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqWrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid or expired OTP.")));
    }

    @Test
    @DisplayName("Weak password is rejected before user/OTP lookup to prevent enumeration")
    void weakPassword_rejectedBeforeOtpOrAccountValidation() throws Exception {
        // Unknown email with short password (< 6 chars)
        ResetPasswordRequest req1 = new ResetPasswordRequest("nonexistent@jaswanti.com", "123456", "123");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));

        // Registered user with short password (< 6 chars)
        String registeredEmail = "recovery_user7@jaswanti.com";
        createOrUpdateTestUser(registeredEmail, "strongPassword123");

        ResetPasswordRequest req2 = new ResetPasswordRequest(registeredEmail, "123456", "short");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));

        // Verify that attempt count was NOT incremented on the registered user (validation preceded OTP check)
        User user = userRepository.findByEmailIgnoreCase(registeredEmail).orElseThrow();
        assertEquals(0, user.getPasswordResetOtpAttempts());
    }

    @Test
    @DisplayName("Endpoints reject query parameters and require JSON request bodies")
    void endpointsRejectQueryParametersAndRequireJsonBody() throws Exception {
        String email = "recovery_user8@jaswanti.com";
        createOrUpdateTestUser(email, "pass123456");

        // 1. Forgot password via query param without JSON body -> rejected (400)
        mockMvc.perform(post("/api/auth/forgot-password?email=" + email))
                .andExpect(status().isBadRequest());

        // 2. Forgot password with empty JSON body and query param -> rejected, does not read query param
        mockMvc.perform(post("/api/auth/forgot-password?email=" + email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 3. Reset password via query params without JSON body -> rejected (400)
        mockMvc.perform(post("/api/auth/reset-password?email=" + email + "&otp=123456&newPassword=newPassword123"))
                .andExpect(status().isBadRequest());

        // 4. Reset password with empty JSON body and query params -> rejected, does not read query params
        mockMvc.perform(post("/api/auth/reset-password?email=" + email + "&otp=123456&newPassword=newPassword123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Flyway migration V3 SQL file is valid and creates expected columns")
    void flywayMigrationV3_isValidAndExecutable() throws Exception {
        // 1. Verify resource exists on classpath
        InputStream migrationStream = getClass().getResourceAsStream("/db/migration/V3__add_password_reset_otp_columns.sql");
        assertNotNull(migrationStream, "V3 migration script must exist in classpath /db/migration/");
        String sql = new String(migrationStream.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("password_reset_otp_hash"));
        assertTrue(sql.contains("password_reset_otp_expiry"));
        assertTrue(sql.contains("password_reset_otp_attempts"));
        assertTrue(sql.contains("password_reset_otp_last_sent_at"));

        // 2. Execute statements on an isolated test table to verify SQL syntax and column addition
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_users_v3_migration (id BIGINT PRIMARY KEY, email VARCHAR(255))");
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                // Strip comments
                String cleaned = trimmed.replaceAll("(?m)^--.*$", "").trim();
                if (!cleaned.isEmpty()) {
                    String testStmt = cleaned.replace("users", "test_users_v3_migration");
                    stmt.execute(testStmt);
                }
            }

            // Verify columns were added, are named correctly, and are queryable
            try (ResultSet rs = stmt.executeQuery("SELECT password_reset_otp_hash, password_reset_otp_expiry, password_reset_otp_attempts, password_reset_otp_last_sent_at FROM test_users_v3_migration")) {
                assertNotNull(rs.getMetaData());
                assertEquals(4, rs.getMetaData().getColumnCount());
            }
        }
    }

    @Test
    @DisplayName("User entity Jackson serialization never includes password reset OTP hash")
    void otpHashNeverSerializedInUserEntity() throws Exception {
        User user = new User();
        user.setEmail("serial_test@jaswanti.com");
        user.setPassword("secret");
        user.setName("Serial Test");
        user.setRole(Role.RESIDENT);
        user.setCreatedAt(null);
        user.setPasswordResetOtpHash("$2a$10$supersecretotphashcannotbeleaked");

        String json = objectMapper.writeValueAsString(user);
        assertFalse(json.contains("supersecretotphashcannotbeleaked"));
        assertFalse(json.contains("passwordResetOtpHash"));
    }
}
