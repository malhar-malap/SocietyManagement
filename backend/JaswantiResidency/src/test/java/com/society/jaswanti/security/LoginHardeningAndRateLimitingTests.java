package com.society.jaswanti.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.dto.AuthRequest;
import com.society.jaswanti.dto.ChangePasswordDto;
import com.society.jaswanti.dto.ProfileSetupDto;
import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.exception.InvalidPasswordException;
import com.society.jaswanti.repository.ActivityLogRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginHardeningAndRateLimitingTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginRateLimiterService loginRateLimiterService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        loginRateLimiterService.resetAll();

        testUser = userRepository.findByEmailIgnoreCase("lockout-test@jaswanti.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("lockout-test@jaswanti.com");
                    u.setPassword(passwordEncoder.encode("ValidPass123"));
                    u.setName("Lockout Test User");
                    u.setRole(Role.RESIDENT);
                    u.setStatus(UserStatus.ACTIVE);
                    u.setActive(true);
                    u.setEmailVerified(true);
                    u.setForcePasswordChange(false);
                    return userRepository.save(u);
                });
    }

    @Test
    @DisplayName("Password rules: reject < 8 chars, reject all-letters, reject all-digits, accept valid combination")
    void passwordRulesValidation() {
        // Less than 8 chars
        assertThatThrownBy(() -> PasswordValidator.validate("Pass1"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least 8 characters");

        // No digits (8+ letters only)
        assertThatThrownBy(() -> PasswordValidator.validate("PasswordOnlyLetters"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least one letter and one digit");

        // No letters (8+ digits only)
        assertThatThrownBy(() -> PasswordValidator.validate("1234567890"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least one letter and one digit");

        // Valid: 8+ chars with at least one letter and one digit
        PasswordValidator.validate("ValidPass1");
        PasswordValidator.validate("Strong2026Secure!");
    }

    @Test
    @DisplayName("changePassword enforces password complexity rules")
    void changePasswordEnforcesPasswordRules() {
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), "ValidPass123", "weak"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least 8 characters");

        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), "ValidPass123", "AllLettersNoDigits"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least one letter and one digit");
    }

    @Test
    @DisplayName("setupProfile enforces password complexity rules when new password is provided")
    void setupProfileEnforcesPasswordRules() {
        ProfileSetupDto dto = new ProfileSetupDto();
        dto.setPersonalEmail("setup-test@example.com");
        dto.setName("New Name");
        dto.setNewPassword("short");

        assertThatThrownBy(() -> userService.setupProfile(testUser.getId(), dto))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    @DisplayName("Brute-force lockout: 5 consecutive failures triggers 429 lockout, resets on success, logs activity")
    void bruteForceLockoutTriggeredAfterFiveFailures() throws Exception {
        AuthRequest badReq = new AuthRequest("lockout-test@jaswanti.com", "WrongPassword123");
        String json = objectMapper.writeValueAsString(badReq);

        // Attempts 1 to 4 should return 401 Unauthorized
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        // 5th failed attempt should trigger lockout and return 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many failed login attempts")));

        // 6th attempt while locked out immediately returns 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());

        // Verify activity log has recorded the lockout
        List<ActivityLog> logs = activityLogRepository.findAll().stream()
                .filter(l -> "LOGIN_LOCKOUT".equals(l.getAction()))
                .toList();
        assertThat(logs).isNotEmpty();
    }

    @Test
    @DisplayName("Successful login resets the failure counter")
    void successfulLoginResetsFailureCounter() throws Exception {
        AuthRequest badReq = new AuthRequest("lockout-test@jaswanti.com", "WrongPassword123");
        String badJson = objectMapper.writeValueAsString(badReq);

        // 3 failed attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badJson))
                    .andExpect(status().isUnauthorized());
        }

        // 1 successful login
        AuthRequest goodReq = new AuthRequest("lockout-test@jaswanti.com", "ValidPass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodReq)))
                .andExpect(status().isOk());

        // Verify remaining attempts are restored to MAX_ATTEMPTS (5)
        int remaining = loginRateLimiterService.getRemainingAttempts("lockout-test@jaswanti.com", "127.0.0.1");
        assertThat(remaining).isEqualTo(5);
    }

    @Test
    @DisplayName("Unknown accounts also experience lockout and do not leak user non-existence")
    void unknownAccountLockoutDoesNotLeakUserExistence() throws Exception {
        AuthRequest unknownReq = new AuthRequest("does-not-exist@jaswanti.com", "WrongPass123");
        String json = objectMapper.writeValueAsString(unknownReq);

        // 1 to 4 return 401
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        // 5th attempt returns 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }
}
