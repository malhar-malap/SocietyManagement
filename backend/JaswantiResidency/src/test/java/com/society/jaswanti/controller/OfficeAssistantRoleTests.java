package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.SystemNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OfficeAssistantRoleTests {

    @Autowired private com.society.jaswanti.support.TestDatabase testDatabase;
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private SystemNotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User officeAssistant;
    private User admin;
    private User masterAdmin;
    private User resident;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        officeAssistant = user("oa-test@example.test", "Office Test", Role.OFFICE_ASSISTANT);
        admin = user("oa-admin@example.test", "Admin Test", Role.ADMIN);
        masterAdmin = user("oa-master@example.test", "Master Test", Role.MASTER_ADMIN);
        resident = user("oa-resident@example.test", "Resident Test", Role.RESIDENT);
    }

    private User user(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPassword("test-password");
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setActive(true);
        return userRepository.save(u);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    @Test
    @DisplayName("Office assistants can use the society, residents and finance sections")
    void canUseSocietyAndFinance() throws Exception {
        for (String path : new String[]{"/api/dashboard/admin", "/api/bills", "/api/transactions", "/api/rooms",
                "/api/bank-accounts", "/api/payments/submissions", "/api/users", "/api/transactions/amendment-requests",
                "/api/notifications"}) {
            int code = mockMvc.perform(get(path).header("Authorization", bearer(officeAssistant)))
                    .andReturn().getResponse().getStatus();
            assertThat(code).as(path).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Office assistants are refused the helpdesk, announcements and the system log")
    void refusedCommunityAndSystemLog() throws Exception {
        for (String path : new String[]{"/api/complaints", "/api/announcements", "/api/announcements/active", "/api/system-logs"}) {
            mockMvc.perform(get(path).header("Authorization", bearer(officeAssistant)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/complaints/{id}/status", 1).param("status", "RESOLVED")
                        .header("Authorization", bearer(officeAssistant)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/complaints/{id}/respond", 1).contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("Authorization", bearer(officeAssistant)))
                .andExpect(status().isForbidden());
        // Admins keep their helpdesk access
        mockMvc.perform(get("/api/complaints").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("The admin dashboard leaves out helpdesk and notice-board figures for office assistants")
    void dashboardHidesCommunityFigures() throws Exception {
        JsonNode oa = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/admin").header("Authorization", bearer(officeAssistant)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(oa.has("openComplaints")).isFalse();
        assertThat(oa.has("activeAnnouncements")).isFalse();
        assertThat(oa.has("pendingAmount")).isTrue();

        JsonNode ad = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/admin").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ad.has("openComplaints")).isTrue();
    }

    @Test
    @DisplayName("Announcement broadcasts are not shown to office assistants")
    void noAnnouncementBroadcasts() throws Exception {
        notificationService.createBroadcast("New announcement", "Water cut", null);
        String oa = mockMvc.perform(get("/api/notifications").header("Authorization", bearer(officeAssistant)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String res = mockMvc.perform(get("/api/notifications").header("Authorization", bearer(resident)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(oa).doesNotContain("Water cut");
        assertThat(res).contains("Water cut");
    }

    @Test
    @DisplayName("Only a Master Admin can make someone an office assistant or create one")
    void onlyMasterAssignsRole() throws Exception {
        mockMvc.perform(put("/api/users/{id}/role", resident.getId()).param("role", "OFFICE_ASSISTANT")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/{id}/role", resident.getId()).param("role", "OFFICE_ASSISTANT")
                        .header("Authorization", bearer(officeAssistant)))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(resident.getId()).orElseThrow().getRole()).isEqualTo(Role.RESIDENT);

        String body = "{\"name\":\"New Office\",\"email\":\"new-office@example.test\",\"role\":\"OFFICE_ASSISTANT\"}";
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findByEmail("new-office@example.test")).isEmpty();

        mockMvc.perform(put("/api/users/{id}/role", resident.getId()).param("role", "OFFICE_ASSISTANT")
                        .header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(resident.getId()).orElseThrow().getRole()).isEqualTo(Role.OFFICE_ASSISTANT);
    }

    @Test
    @DisplayName("Regular admins cannot edit an office assistant's account details")
    void adminsCannotEditOfficeAssistant() throws Exception {
        mockMvc.perform(put("/api/users/{id}", officeAssistant.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hijack@example.test\"}")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(officeAssistant.getId()).orElseThrow().getEmail()).isEqualTo("oa-test@example.test");
    }
}
