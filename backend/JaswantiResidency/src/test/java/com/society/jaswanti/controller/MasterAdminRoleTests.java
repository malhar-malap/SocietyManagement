package com.society.jaswanti.controller;

import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MasterAdminRoleTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.society.jaswanti.support.TestDatabase testDatabase;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User masterAdmin;
    private User target;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        admin = findOrCreate("role-test-admin@jaswanti.com", "Role Test Admin", Role.ADMIN);
        masterAdmin = findOrCreate("role-test-master@jaswanti.com", "Role Test Master", Role.MASTER_ADMIN);
        target = findOrCreate("role-test-target@jaswanti.com", "Role Test Target", Role.RESIDENT);
        target.setRole(Role.RESIDENT);
        target.setStatus(UserStatus.ACTIVE);
        target.setActive(true);
        userRepository.save(target);
    }

    @Test
    void masterAdminAuthorityAndMigrationAreDefined() throws Exception {
        assertThat(Role.valueOf("MASTER_ADMIN")).isEqualTo(Role.MASTER_ADMIN);
        assertThat(masterAdmin.getAuthorities())
                .anyMatch(authority -> "ROLE_MASTER_ADMIN".equals(authority.getAuthority()));

        String migration = new String(new ClassPathResource(
                "db/migration/V4__add_master_admin_role.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(migration).contains("MASTER_ADMIN").contains("ALTER TABLE users");
    }

    @Test
    void adminCannotChangeRolesOrStatuses() throws Exception {
        String token = jwtService.generateToken(admin);

        mockMvc.perform(put("/api/users/{id}/role", target.getId())
                        .param("role", "MASTER_ADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/{id}/status", target.getId())
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole()).isEqualTo(Role.RESIDENT);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void masterAdminCanChangeRoleAndStatus() throws Exception {
        String token = jwtService.generateToken(masterAdmin);

        mockMvc.perform(put("/api/users/{id}/role", target.getId())
                        .param("role", "ADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/users/{id}/status", target.getId())
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
        assertThat(updated.getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void lastActiveMasterAdminCannotBeDemotedBlockedOrRemoved() throws Exception {
        String token = jwtService.generateToken(masterAdmin);

        mockMvc.perform(put("/api/users/{id}/role", masterAdmin.getId())
                        .param("role", "ADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/users/{id}/status", masterAdmin.getId())
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/users/{id}", masterAdmin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        User unchanged = userRepository.findById(masterAdmin.getId()).orElseThrow();
        assertThat(unchanged.getRole()).isEqualTo(Role.MASTER_ADMIN);
        assertThat(unchanged.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(unchanged.isActive()).isTrue();
    }

    private User findOrCreate(String email, String name, Role role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName(name);
        user.setPassword("test-password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        return userRepository.save(user);
    }
}
