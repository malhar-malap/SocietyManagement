package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.ActivityLogRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.security.LoginRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResidentOnboardingTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ActivityLogRepository activityLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private LoginRateLimiterService limiter;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String run;
    private User master;
    private User admin;

    @BeforeEach
    void setUp() {
        limiter.resetAll();
        run = UUID.randomUUID().toString().substring(0, 8);
        master = user("onb-master-" + run + "@example.test", Role.MASTER_ADMIN, "Password123", false, UserStatus.ACTIVE);
        admin = user("onb-admin-" + run + "@example.test", Role.ADMIN, "Password123", false, UserStatus.ACTIVE);
    }

    private User user(String email, Role role, String password, boolean forceChange, UserStatus status) {
        User u = new User();
        u.setEmail(email);
        u.setName("User " + email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(role);
        u.setStatus(status);
        u.setActive(status == UserStatus.ACTIVE);
        u.setForcePasswordChange(forceChange);
        return userRepository.save(u);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    private int login(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private JsonNode create(User actor, String json, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post("/api/users").header("Authorization", bearer(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    @Test
    @DisplayName("An admin can add a resident; a one-time temporary password is generated and works once")
    void adminAddsResidentWithGeneratedPassword() throws Exception {
        String email = "new-resident-" + run + "@example.test";
        JsonNode created = create(admin, "{\"name\":\"New Resident\",\"email\":\"" + email + "\",\"phone\":\"9000000000\"}", 201);

        String temp = created.get("temporaryPassword").asText();
        assertThat(temp).hasSize(12).matches(".*[A-Za-z].*").matches(".*\\d.*");
        assertThat(created.get("role").asText()).isEqualTo("RESIDENT");

        User stored = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(stored.isForcePasswordChange()).isTrue();
        assertThat(stored.getPassword()).doesNotContain(temp);
        assertThat(login(email, temp)).isEqualTo(200);

        // The temporary password never reaches the system log
        assertThat(activityLogRepository.findAll()).noneMatch(l -> l.getDescription() != null && l.getDescription().contains(temp));
        assertThat(activityLogRepository.findAll()).anyMatch(l -> "CREATE_USER".equals(l.getAction())
                && l.getDescription().contains("generated temporary password"));
    }

    @Test
    @DisplayName("An admin-set initial password must meet the policy and is not echoed back")
    void adminSetPassword() throws Exception {
        create(admin, "{\"name\":\"Weak\",\"email\":\"weak-" + run + "@example.test\",\"password\":\"short\"}", 400);

        String email = "strong-" + run + "@example.test";
        JsonNode created = create(admin, "{\"name\":\"Strong\",\"email\":\"" + email + "\",\"password\":\"Initial2026\"}", 201);
        assertThat(created.has("temporaryPassword")).isFalse();
        assertThat(login(email, "Initial2026")).isEqualTo(200);
    }

    @Test
    @DisplayName("Only a Master Admin can create admins; nobody can create a Master Admin; emails must be unique")
    void roleAndUniquenessRules() throws Exception {
        create(admin, "{\"name\":\"Sneaky\",\"email\":\"sneaky-" + run + "@example.test\",\"role\":\"ADMIN\"}", 403);
        create(master, "{\"name\":\"Ops Admin\",\"email\":\"ops-" + run + "@example.test\",\"role\":\"ADMIN\"}", 201);
        create(master, "{\"name\":\"Second Master\",\"email\":\"m2-" + run + "@example.test\",\"role\":\"MASTER_ADMIN\"}", 403);
        create(admin, "{\"name\":\"Dup\",\"email\":\"" + admin.getEmail().toUpperCase() + "\"}", 400);
    }

    @Test
    @DisplayName("Admin password reset issues a temporary password and ends the old password and sessions")
    void adminResetsResidentPassword() throws Exception {
        User resident = user("reset-" + run + "@example.test", Role.RESIDENT, "OldPassword1", false, UserStatus.ACTIVE);
        String oldToken = bearer(resident);

        String body = mockMvc.perform(post("/api/users/" + resident.getId() + "/reset-password").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String temp = objectMapper.readTree(body).get("temporaryPassword").asText();

        assertThat(login(resident.getEmail(), "OldPassword1")).isNotEqualTo(200);
        assertThat(login(resident.getEmail(), temp)).isEqualTo(200);
        mockMvc.perform(get("/api/users/" + resident.getId()).header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
        assertThat(userRepository.findById(resident.getId()).orElseThrow().isForcePasswordChange()).isTrue();

        // An admin cannot reset another administrator; a Master Admin can
        User otherAdmin = user("reset-admin-" + run + "@example.test", Role.ADMIN, "Password123", false, UserStatus.ACTIVE);
        mockMvc.perform(post("/api/users/" + otherAdmin.getId() + "/reset-password").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/" + otherAdmin.getId() + "/reset-password").header("Authorization", bearer(master)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isString());
    }

    @Test
    @DisplayName("Reactivating a never-onboarded account issues a new temporary password; its old default stops working")
    void reactivationDoesNotRestoreDefaultPassword() throws Exception {
        User seeded = user("seeded-" + run + "@example.test", Role.RESIDENT, "Temp_C-999", true, UserStatus.INACTIVE);

        String body = mockMvc.perform(put("/api/users/" + seeded.getId() + "/status").param("status", "ACTIVE")
                        .header("Authorization", bearer(master)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String temp = objectMapper.readTree(body).get("temporaryPassword").asText();

        assertThat(login(seeded.getEmail(), "Temp_C-999")).isNotEqualTo(200);
        assertThat(login(seeded.getEmail(), temp)).isEqualTo(200);

        // An onboarded account keeps its own password when reactivated
        User onboarded = user("onboarded-" + run + "@example.test", Role.RESIDENT, "MyOwnPass9", false, UserStatus.INACTIVE);
        String body2 = mockMvc.perform(put("/api/users/" + onboarded.getId() + "/status").param("status", "ACTIVE")
                        .header("Authorization", bearer(master)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body2).has("temporaryPassword")).isFalse();
        assertThat(login(onboarded.getEmail(), "MyOwnPass9")).isEqualTo(200);
    }
}
