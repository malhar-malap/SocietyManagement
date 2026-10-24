package com.society.jaswanti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.Complaint;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationAndMassAssignmentTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private com.society.jaswanti.repository.SystemNotificationRepository notificationRepository;
    @Autowired private com.society.jaswanti.repository.AnnouncementRepository announcementRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String run;
    private User master;
    private User admin;
    private User otherAdmin;
    private User resident;
    private final java.util.List<Long> createdRooms = new java.util.ArrayList<>();
    private final java.util.List<Long> createdComplaints = new java.util.ArrayList<>();
    private final java.util.List<Long> createdNotifications = new java.util.ArrayList<>();
    private final java.util.List<Long> createdAnnouncements = new java.util.ArrayList<>();

    /** Other test classes count rooms and delete users, so leave nothing behind. */
    @AfterEach
    void cleanUp() {
        createdComplaints.forEach(complaintRepository::deleteById);
        createdNotifications.forEach(notificationRepository::deleteById);
        createdAnnouncements.forEach(announcementRepository::deleteById);
        roomRepository.findAll().stream()
                .filter(r -> r.getRoomNumber() != null && r.getRoomNumber().endsWith("-" + run))
                .forEach(roomRepository::delete);
    }

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        master = user("mass-master-" + run + "@example.test", Role.MASTER_ADMIN);
        admin = user("mass-admin-" + run + "@example.test", Role.ADMIN);
        otherAdmin = user("mass-admin2-" + run + "@example.test", Role.ADMIN);
        resident = user("mass-resident-" + run + "@example.test", Role.RESIDENT);
    }

    private User user(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setName("User " + email);
        u.setPassword(passwordEncoder.encode("Password123"));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setActive(true);
        u.setForcePasswordChange(false);
        return userRepository.save(u);
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.generateToken(u);
    }

    @Test
    @DisplayName("An admin cannot change a Master Admin's (or another admin's) email; a Master Admin can")
    void adminCannotEditAdministrators() throws Exception {
        String hijack = "{\"email\":\"attacker-" + run + "@example.test\"}";
        mockMvc.perform(put("/api/users/" + master.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(hijack))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/" + otherAdmin.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(hijack))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(master.getId()).orElseThrow().getEmail()).isEqualTo(master.getEmail());

        mockMvc.perform(put("/api/users/" + otherAdmin.getId()).header("Authorization", bearer(master))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renamed Admin\"}"))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(otherAdmin.getId()).orElseThrow().getName()).isEqualTo("Renamed Admin");
    }

    @Test
    @DisplayName("Editing a resident's email rejects duplicates and bad formats; a valid change ends old sessions")
    void residentEmailEdits() throws Exception {
        mockMvc.perform(put("/api/users/" + resident.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + admin.getEmail().toUpperCase() + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/" + resident.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/users/" + resident.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"New.Resident-" + run + "@Example.test\"}"))
                .andExpect(status().isOk());
        User updated = userRepository.findById(resident.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new.resident-" + run + "@example.test");
        assertThat(updated.getTokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("The raw POST /api/transactions endpoint, which bypassed the ledger, no longer exists")
    void rawTransactionEndpointRemoved() throws Exception {
        int status = mockMvc.perform(post("/api/transactions").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":999999,\"status\":\"SUCCESS\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(404, 405);
    }

    @Test
    @DisplayName("Creating a room ignores a client-supplied id and certificate data; duplicates are rejected")
    void createRoomIgnoresDangerousFields() throws Exception {
        Room existing = new Room();
        existing.setRoomNumber("MA-" + run);
        existing.setWing("M");
        existing.setFloor(1);
        existing = roomRepository.save(existing);

        String body = "{\"id\":" + existing.getId() + ",\"roomNumber\":\"MB-" + run + "\",\"wing\":\"M\",\"floor\":2,"
                + "\"shareCertificateNumber\":\"FORGED-1\",\"nocApplicable\":true,\"nocAmount\":500}";
        String response = mockMvc.perform(post("/api/rooms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long newId = objectMapper.readTree(response).get("id").asLong();
        assertThat(newId).isNotEqualTo(existing.getId());
        assertThat(roomRepository.findById(existing.getId()).orElseThrow().getRoomNumber()).isEqualTo("MA-" + run);
        Room created = roomRepository.findById(newId).orElseThrow();
        assertThat(created.getShareCertificateNumber()).isNull();
        assertThat(created.isNocApplicable()).isTrue();

        mockMvc.perform(post("/api/rooms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roomNumber\":\"MB-" + run + "\",\"wing\":\"M\",\"floor\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A partial room edit keeps the NOC setting; only a Master Admin may change certificate details")
    void roomEditRules() throws Exception {
        Room room = new Room();
        room.setRoomNumber("MC-" + run);
        room.setWing("M");
        room.setFloor(3);
        room.setNocApplicable(true);
        room.setNocAmount(new BigDecimal("750.00"));
        room = roomRepository.save(room);

        mockMvc.perform(put("/api/rooms/" + room.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"wing\":\"N\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nocApplicable").value(true));

        mockMvc.perform(put("/api/rooms/" + room.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"shareCertificateNumber\":\"SC-9\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/rooms/" + room.getId()).header("Authorization", bearer(master))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"shareCertificateNumber\":\"SC-9\"}"))
                .andExpect(status().isOk());
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getShareCertificateNumber()).isEqualTo("SC-9");
    }

    @Test
    @DisplayName("Malformed requests get 4xx with a safe message instead of a 500 that echoes internals")
    void malformedRequestsAreClientErrors() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/bills/not-a-number")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
        mockMvc.perform(put("/api/complaints/1/status").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/system-logs").header("Authorization", bearer(master)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("An announcement's author is always the signed-in user, whatever adminId the client sends")
    void announcementAuthorCannotBeSpoofed() throws Exception {
        String body = mockMvc.perform(post("/api/announcements").param("adminId", String.valueOf(otherAdmin.getId()))
                        .header("Authorization", bearer(master)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Spoof check " + run + "\",\"description\":\"x\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(body);
        createdAnnouncements.add(json.get("id").asLong());
        assertThat(json.get("createdById").asLong()).isEqualTo(master.getId());
    }

    @Test
    @DisplayName("Users can only mark their own notifications read; a broadcast is not marked read for everyone")
    void notificationReadOwnership() throws Exception {
        com.society.jaswanti.entity.SystemNotification mine = notificationRepository.save(
                new com.society.jaswanti.entity.SystemNotification(resident, "Mine", "m", null));
        com.society.jaswanti.entity.SystemNotification theirs = notificationRepository.save(
                new com.society.jaswanti.entity.SystemNotification(admin, "Theirs", "t", null));
        com.society.jaswanti.entity.SystemNotification broadcast = notificationRepository.save(
                new com.society.jaswanti.entity.SystemNotification(null, "Everyone", "b", null));
        createdNotifications.addAll(java.util.List.of(mine.getId(), theirs.getId(), broadcast.getId()));

        mockMvc.perform(put("/api/notifications/" + theirs.getId() + "/read").header("Authorization", bearer(resident)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/notifications/" + mine.getId() + "/read").header("Authorization", bearer(resident)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/notifications/" + broadcast.getId() + "/read").header("Authorization", bearer(resident)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findById(theirs.getId()).orElseThrow().isRead()).isFalse();
        assertThat(notificationRepository.findById(mine.getId()).orElseThrow().isRead()).isTrue();
        assertThat(notificationRepository.findById(broadcast.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    @DisplayName("An unknown helpdesk status is rejected with 400; deleting a missing submission is 404")
    void complaintStatusValidation() throws Exception {
        Complaint c = new Complaint();
        c.setResident(resident);
        c.setCategory("Plumbing");
        c.setDescription("Leak");
        c = complaintRepository.save(c);
        createdComplaints.add(c.getId());

        mockMvc.perform(put("/api/complaints/" + c.getId() + "/status").param("status", "DONE_DONE")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
        assertThat(complaintRepository.findById(c.getId()).orElseThrow().getStatus().name()).isEqualTo("OPEN");

        mockMvc.perform(put("/api/complaints/" + c.getId() + "/status").param("status", "resolved")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/complaints/987654321").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }
}
