package com.society.jaswanti.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountTakeoverAndLogoutTests {

    @Autowired private com.society.jaswanti.support.TestDatabase testDatabase;
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginRateLimiterService loginRateLimiterService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User master;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        master = user("takeover-master@example.test", Role.MASTER_ADMIN, false);
    }

    private User user(String email, Role role, boolean pendingSetup) {
        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setPassword(passwordEncoder.encode("Original123"));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setActive(true);
        u.setForcePasswordChange(pendingSetup);
        return userRepository.save(u);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    private int setup(User target, User actor) throws Exception {
        return mockMvc.perform(post("/api/users/{id}/setup", target.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personalEmail\":\"attacker@example.test\",\"newPassword\":\"Hijacked123\"}")
                        .header("Authorization", bearer(actor)))
                .andReturn().getResponse().getStatus();
    }

    private void assertPasswordUnchanged(User u) {
        User fresh = userRepository.findById(u.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("Original123", fresh.getPassword())).isTrue();
        assertThat(fresh.getPendingEmail()).isNull();
    }

    @Test
    @DisplayName("Staff cannot run first-login setup on someone else's account (no Master Admin takeover)")
    void staffCannotSetUpAnotherAccount() throws Exception {
        for (Role role : new Role[]{Role.ADMIN, Role.OFFICE_ASSISTANT}) {
            User staff = user("takeover-" + role.name().toLowerCase() + "@example.test", role, false);
            assertThat(setup(master, staff)).as(role.name()).isEqualTo(403);
            mockMvc.perform(post("/api/users/{id}/verify-setup", master.getId()).param("token", "000000")
                            .header("Authorization", bearer(staff)))
                    .andExpect(status().isForbidden());
        }
        assertPasswordUnchanged(master);

        // A brand-new staff account that has not finished setup cannot be claimed by another admin either
        User pending = user("takeover-pending@example.test", Role.OFFICE_ASSISTANT, true);
        User admin = user("takeover-admin2@example.test", Role.ADMIN, false);
        assertThat(setup(pending, admin)).isEqualTo(403);
        assertPasswordUnchanged(pending);
    }

    @Test
    @DisplayName("Setup works only while the account is still pending first sign-in")
    void setupOnlyWhilePending() throws Exception {
        User pending = user("takeover-new@example.test", Role.RESIDENT, true);
        assertThat(setup(pending, pending)).isEqualTo(200);

        User done = user("takeover-done@example.test", Role.RESIDENT, false);
        assertThat(setup(done, done)).isEqualTo(403);
        assertPasswordUnchanged(done);
    }

    @Test
    @DisplayName("Signing out revokes the session's tokens on the server")
    void logoutRevokesTokens() throws Exception {
        User resident = user("takeover-logout@example.test", Role.RESIDENT, false);
        loginRateLimiterService.recordSuccessfulLogin(resident.getEmail(), "127.0.0.1");
        JsonNode tokens = objectMapper.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + resident.getEmail() + "\",\"password\":\"Original123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String access = tokens.get("accessToken").asText();
        String refresh = tokens.get("refreshToken").asText();

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + access)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // Garbage or missing tokens are answered the same way and change nothing
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"nope\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }
}
