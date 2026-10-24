package com.society.jaswanti.security;

import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.PasswordResetService;
import com.society.jaswanti.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtHardeningTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User residentUser;
    private User masterAdminUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        residentUser = new User();
        residentUser.setName("JWT Test Resident");
        residentUser.setEmail("jwt.resident@example.com");
        residentUser.setPassword(passwordEncoder.encode("password123"));
        residentUser.setRole(Role.RESIDENT);
        residentUser.setStatus(UserStatus.ACTIVE);
        residentUser.setActive(true);
        residentUser.setTokenVersion(0);
        residentUser = userRepository.save(residentUser);

        masterAdminUser = new User();
        masterAdminUser.setName("JWT Test Master");
        masterAdminUser.setEmail("jwt.master@example.com");
        masterAdminUser.setPassword(passwordEncoder.encode("password123"));
        masterAdminUser.setRole(Role.MASTER_ADMIN);
        masterAdminUser.setStatus(UserStatus.ACTIVE);
        masterAdminUser.setActive(true);
        masterAdminUser.setTokenVersion(0);
        masterAdminUser = userRepository.save(masterAdminUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Refresh token is rejected on normal authenticated endpoints")
    void refreshTokenRejectedOnNormalEndpoints() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(residentUser);

        // Filter must reject refresh token (not authenticated -> 401 or 403 on protected endpoint)
        mockMvc.perform(get("/api/announcements/active")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access token is rejected on /api/auth/refresh endpoint")
    void accessTokenRejectedOnRefreshEndpoint() throws Exception {
        String accessToken = jwtService.generateToken(residentUser);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid refresh token rotates and returns new access and refresh tokens")
    void validRefreshTokenRotates() throws Exception {
        String initialRefreshToken = jwtService.generateRefreshToken(residentUser);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("RESIDENT"));
    }

    @Test
    @DisplayName("Blocked/inactive user is rejected on both normal endpoints and /refresh")
    void blockedUserRejectedOnBothEndpoints() throws Exception {
        String accessToken = jwtService.generateToken(residentUser);
        String refreshToken = jwtService.generateRefreshToken(residentUser);

        // Block the user
        residentUser.setStatus(UserStatus.BLOCKED);
        residentUser.setActive(false);
        userRepository.save(residentUser);

        // Access token rejected on normal endpoint
        mockMvc.perform(get("/api/announcements/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // Refresh token rejected on /refresh
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tokens are invalidated after role change")
    void tokensInvalidatedAfterRoleChange() throws Exception {
        String accessTokenBefore = jwtService.generateToken(residentUser);
        String refreshTokenBefore = jwtService.generateRefreshToken(residentUser);

        // Verify access token worked before role change
        mockMvc.perform(get("/api/announcements/active")
                        .header("Authorization", "Bearer " + accessTokenBefore))
                .andExpect(status().isOk());

        // Master admin changes role to ADMIN
        Authentication masterAuth = new UsernamePasswordAuthenticationToken(masterAdminUser, null, masterAdminUser.getAuthorities());
        userService.updateUserRole(residentUser.getId(), "ADMIN", masterAuth);

        User updatedUser = userRepository.findById(residentUser.getId()).orElseThrow();
        assertThat(updatedUser.getTokenVersion()).isGreaterThan(0);

        // Old access token should now be rejected
        mockMvc.perform(get("/api/announcements/active")
                        .header("Authorization", "Bearer " + accessTokenBefore))
                .andExpect(status().isUnauthorized());

        // Old refresh token should also be rejected
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshTokenBefore + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tokens are invalidated after password reset")
    void tokensInvalidatedAfterPasswordReset() throws Exception {
        String accessTokenBefore = jwtService.generateToken(residentUser);
        String refreshTokenBefore = jwtService.generateRefreshToken(residentUser);

        // Initiate reset and fake OTP state
        residentUser.setPasswordResetOtpHash(passwordEncoder.encode("123456"));
        residentUser.setPasswordResetOtpExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        residentUser.setPasswordResetOtpAttempts(0);
        userRepository.save(residentUser);

        passwordResetService.resetPassword(residentUser.getEmail(), "123456", "newSecurePassword1");

        User updatedUser = userRepository.findById(residentUser.getId()).orElseThrow();
        assertThat(updatedUser.getTokenVersion()).isGreaterThan(0);

        // Old tokens are now invalid
        mockMvc.perform(get("/api/announcements/active")
                        .header("Authorization", "Bearer " + accessTokenBefore))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshTokenBefore + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
