package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class TokenRevocationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoSpyBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        doNothing().when(emailService).sendEmail(any(), any(), any());
    }

    private User createOrUpdateUser(String email, Role role, String plainPassword) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName("Revocation " + email);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        user.setForcePasswordChange(false);
        user.setTokenVersion(0);
        return userRepository.save(user);
    }

    private String changePasswordBody(String oldPassword, String newPassword) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("oldPassword", oldPassword, "newPassword", newPassword));
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken));
    }

    @Test
    @DisplayName("Refresh token cannot be used as an access token")
    void refreshTokenRejectedAsAccessToken() throws Exception {
        User user = createOrUpdateUser("revoke_typ@jaswanti.com", Role.RESIDENT, "password123");
        String refreshToken = jwtService.generateRefreshToken(user);

        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access token cannot be used to refresh")
    void accessTokenRejectedByRefreshEndpoint() throws Exception {
        User user = createOrUpdateUser("revoke_refresh_typ@jaswanti.com", Role.RESIDENT, "password123");
        String accessToken = jwtService.generateToken(user);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh rotates the refresh token and returns a working access token")
    void refreshReturnsNewTokenPair() throws Exception {
        User user = createOrUpdateUser("revoke_rotate@jaswanti.com", Role.RESIDENT, "password123");
        String refreshToken = jwtService.generateRefreshToken(user);

        String body = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(jwtService.extractTokenType(json.get("refreshToken").asText())).isEqualTo("refresh");

        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + json.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Changing own password revokes old tokens and returns a fresh working pair")
    void changePasswordRevokesOldTokensAndReturnsNewOnes() throws Exception {
        User user = createOrUpdateUser("revoke_change@jaswanti.com", Role.RESIDENT, "oldPassword123");
        String oldAccess = jwtService.generateToken(user);
        String oldRefresh = jwtService.generateRefreshToken(user);

        String body = mockMvc.perform(post("/api/users/" + user.getId() + "/change-password")
                        .header("Authorization", "Bearer " + oldAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("oldPassword123", "newPassword456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);

        // Old tokens are dead
        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(oldRefresh)))
                .andExpect(status().isUnauthorized());

        // New tokens work
        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + json.get("accessToken").asText()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(json.get("refreshToken").asText())))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(user.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Wrong old password is rejected and does not revoke tokens")
    void changePasswordWithWrongOldPasswordKeepsSession() throws Exception {
        User user = createOrUpdateUser("revoke_wrong@jaswanti.com", Role.RESIDENT, "oldPassword123");
        String access = jwtService.generateToken(user);

        mockMvc.perform(post("/api/users/" + user.getId() + "/change-password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("notTheRightOne", "newPassword456")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin changing another user's password does not receive that user's tokens")
    void adminChangingOtherPasswordGetsNoTokens() throws Exception {
        User admin = createOrUpdateUser("revoke_admin@jaswanti.com", Role.ADMIN, "adminPassword123");
        User resident = createOrUpdateUser("revoke_target@jaswanti.com", Role.RESIDENT, "oldPassword123");

        String body = mockMvc.perform(post("/api/users/" + resident.getId() + "/change-password")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("oldPassword123", "newPassword456")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo("Password changed successfully");
        assertThat(body).doesNotContain("accessToken");
    }

    @Test
    @DisplayName("Blocking a user revokes their existing access and refresh tokens")
    void blockingUserRevokesTokens() throws Exception {
        User masterAdmin = createOrUpdateUser("revoke_master@jaswanti.com", Role.MASTER_ADMIN, "masterPassword123");
        User resident = createOrUpdateUser("revoke_blocked@jaswanti.com", Role.RESIDENT, "password123");
        String access = jwtService.generateToken(resident);
        String refresh = jwtService.generateRefreshToken(resident);

        mockMvc.perform(put("/api/users/" + resident.getId() + "/status")
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + jwtService.generateToken(masterAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + resident.getId())
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Setup verification OTP is never written to the logs")
    void setupOtpNotLogged(CapturedOutput output) throws Exception {
        User user = createOrUpdateUser("revoke_setup@jaswanti.com", Role.RESIDENT, "password123");
        String access = jwtService.generateToken(user);

        String setupPayload = """
                {
                    "name": "Setup Resident",
                    "phone": "9876543210",
                    "personalEmail": "revoke_setup_new@example.com",
                    "newPassword": "NewStrongPassword123"
                }
                """;
        mockMvc.perform(post("/api/users/" + user.getId() + "/setup")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setupPayload))
                .andExpect(status().isOk());

        String otp = userRepository.findById(user.getId()).orElseThrow().getEmailVerificationToken();
        assertThat(otp).isNotBlank();

        mockMvc.perform(post("/api/users/" + user.getId() + "/verify-setup")
                        .param("token", "0000".equals(otp) ? "1111" : "0000")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isBadRequest());

        assertThat(output.getAll())
                .contains("Generated setup verification OTP for user ID " + user.getId())
                .doesNotContain("revoke_setup_new@example.com): " + otp)
                .doesNotContain("stored='" + otp + "'");
    }
}
