package com.society.jaswanti.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.entity.ActivityLog;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemLogTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.society.jaswanti.support.TestDatabase testDatabase;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ActivityLogRepository activityLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private LoginRateLimiterService loginRateLimiterService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User masterAdmin;
    private User admin;
    private User resident;

    @BeforeEach
    void setUp() {
        testDatabase.wipe();
        activityLogRepository.deleteAll();
        String run = UUID.randomUUID().toString().substring(0, 8);
        masterAdmin = user("syslog-master-" + run + "@example.test", Role.MASTER_ADMIN, "Sys Master");
        admin = user("syslog-admin-" + run + "@example.test", Role.ADMIN, "Sys Admin");
        resident = user("syslog-resident-" + run + "@example.test", Role.RESIDENT, "Sys Resident");
    }

    private User user(String email, Role role, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
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

    private List<ActivityLog> logs(String action) {
        return activityLogRepository.findAll().stream()
                .filter(l -> action.equals(l.getAction()))
                .sorted(Comparator.comparing(ActivityLog::getId))
                .toList();
    }

    private long createAnnouncement(String title) throws Exception {
        String body = mockMvc.perform(post("/api/announcements")
                        .header("Authorization", bearer(masterAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"Details\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("Posting an announcement is logged with actor, target title and success")
    void createAnnouncementIsLogged() throws Exception {
        long id = createAnnouncement("Water supply shutdown");

        List<ActivityLog> entries = logs("CREATE_ANNOUNCEMENT");
        assertThat(entries).hasSize(1);
        ActivityLog log = entries.get(0);
        assertThat(log.getOutcome()).isEqualTo(ActivityLog.OUTCOME_SUCCESS);
        assertThat(log.getModule()).isEqualTo("ANNOUNCEMENTS");
        assertThat(log.getActorEmail()).isEqualTo(masterAdmin.getEmail());
        assertThat(log.getActorRole()).isEqualTo("MASTER_ADMIN");
        assertThat(log.getUser().getId()).isEqualTo(masterAdmin.getId());
        assertThat(log.getTargetType()).isEqualTo("ANNOUNCEMENT");
        assertThat(log.getTargetId()).isEqualTo(String.valueOf(id));
        assertThat(log.getTargetLabel()).isEqualTo("\"Water supply shutdown\"");
        assertThat(log.getHttpMethod()).isEqualTo("POST");
        assertThat(log.getRequestPath()).isEqualTo("/api/announcements");
        assertThat(log.getDescription()).isEqualTo("Posted an announcement");
    }

    @Test
    @DisplayName("Deleting an announcement keeps the deleted title in the log")
    void deleteAnnouncementKeepsLabel() throws Exception {
        long id = createAnnouncement("AGM on Sunday");

        mockMvc.perform(delete("/api/announcements/" + id).header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isNoContent());

        ActivityLog log = logs("DELETE_ANNOUNCEMENT").get(0);
        assertThat(log.getOutcome()).isEqualTo(ActivityLog.OUTCOME_SUCCESS);
        assertThat(log.getTargetLabel()).isEqualTo("\"AGM on Sunday\"");
    }

    @Test
    @DisplayName("Editing an announcement records both the old title (target) and the new title")
    void editAnnouncementRecordsOldAndNewTitle() throws Exception {
        long id = createAnnouncement("Gym closed Monday");

        mockMvc.perform(put("/api/announcements/" + id)
                        .header("Authorization", bearer(masterAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Gym closed Tuesday\"}"))
                .andExpect(status().isOk());

        ActivityLog log = logs("UPDATE_ANNOUNCEMENT").get(0);
        assertThat(log.getTargetLabel()).isEqualTo("\"Gym closed Monday\"");
        assertThat(log.getDescription()).isEqualTo("Edited an announcement; title is now \"Gym closed Tuesday\"");
    }

    @Test
    @DisplayName("A refused attempt is logged as DENIED against the person who tried")
    void deniedAttemptIsLogged() throws Exception {
        long id = createAnnouncement("Parking rules");

        mockMvc.perform(delete("/api/announcements/" + id).header("Authorization", bearer(resident)))
                .andExpect(status().isForbidden());

        ActivityLog log = logs("DELETE_ANNOUNCEMENT").get(0);
        assertThat(log.getOutcome()).isEqualTo(ActivityLog.OUTCOME_DENIED);
        assertThat(log.getActorEmail()).isEqualTo(resident.getEmail());
        assertThat(log.getDescription()).contains("denied (HTTP 403)");
    }

    @Test
    @DisplayName("Changing a user's status records the before and after values")
    void statusChangeRecordsBeforeAndAfter() throws Exception {
        mockMvc.perform(put("/api/users/" + resident.getId() + "/status")
                        .param("status", "BLOCKED")
                        .header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isOk());

        ActivityLog log = logs("UPDATE_USER_STATUS").get(0);
        assertThat(log.getDescription()).isEqualTo("Changed status of Sys Resident from ACTIVE to BLOCKED");
        assertThat(log.getTargetLabel()).isEqualTo("Sys Resident <" + resident.getEmail() + ">");
    }

    @Test
    @DisplayName("Failed sign-ins are logged without the password; successful sign-ins are not logged")
    void onlyFailedSignInsAreLogged() throws Exception {
        loginRateLimiterService.recordSuccessfulLogin(admin.getEmail(), "127.0.0.1");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + admin.getEmail() + "\",\"password\":\"WrongSecret999\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + admin.getEmail() + "\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk());

        List<ActivityLog> entries = logs("LOGIN");
        assertThat(entries).hasSize(1);
        ActivityLog failed = entries.get(0);
        assertThat(failed.getOutcome()).isEqualTo(ActivityLog.OUTCOME_DENIED);
        assertThat(failed.getUser()).isNull();
        assertThat(failed.getActorEmail()).isEqualTo(admin.getEmail());
        assertThat(failed.getDescription()).contains("Failed sign-in attempt").doesNotContain("WrongSecret999");
    }

    @Test
    @DisplayName("Only Master Admins can read the system log; admins and residents are refused")
    void systemLogsAreMasterAdminOnly() throws Exception {
        createAnnouncement("Visibility check");

        mockMvc.perform(get("/api/system-logs").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system-logs").header("Authorization", bearer(resident)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system-logs"))
                .andExpect(status().isUnauthorized());

        String body = mockMvc.perform(get("/api/system-logs").header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andReturn().getResponse().getContentAsString();
        // The response is a DTO: no nested User entity and nothing sensitive
        assertThat(body).doesNotContain("\"user\"").doesNotContain("password").doesNotContain("tokenVersion");
        assertThat(objectMapper.readTree(body).get("content").get(0).get("action").asText()).isEqualTo("CREATE_ANNOUNCEMENT");
    }

    @Test
    @DisplayName("System log search filters by module, outcome and free text, newest first")
    void systemLogsFilter() throws Exception {
        createAnnouncement("Lift maintenance");
        long id = createAnnouncement("Diwali celebration");
        mockMvc.perform(delete("/api/announcements/" + id).header("Authorization", bearer(resident)))
                .andExpect(status().isForbidden());

        JsonNode denied = objectMapper.readTree(mockMvc.perform(get("/api/system-logs")
                        .param("outcome", "DENIED").header("Authorization", bearer(masterAdmin)))
                .andReturn().getResponse().getContentAsString());
        assertThat(denied.get("totalElements").asInt()).isEqualTo(1);
        assertThat(denied.get("content").get(0).get("actorEmail").asText()).isEqualTo(resident.getEmail());

        JsonNode text = objectMapper.readTree(mockMvc.perform(get("/api/system-logs")
                        .param("q", "lift").param("module", "ANNOUNCEMENTS").header("Authorization", bearer(masterAdmin)))
                .andReturn().getResponse().getContentAsString());
        assertThat(text.get("totalElements").asInt()).isEqualTo(1);
        assertThat(text.get("content").get(0).get("targetLabel").asText()).contains("Lift maintenance");

        JsonNode all = objectMapper.readTree(mockMvc.perform(get("/api/system-logs").param("size", "500")
                        .header("Authorization", bearer(masterAdmin)))
                .andReturn().getResponse().getContentAsString());
        assertThat(all.get("size").asInt()).isEqualTo(100);
        assertThat(all.get("content").get(0).get("outcome").asText()).isEqualTo("DENIED");
    }

    @Test
    @DisplayName("CSV export works, neutralises spreadsheet formulas, and is itself logged")
    void exportCsv() throws Exception {
        masterAdmin.setName("=SUM(1+1)");
        masterAdmin = userRepository.save(masterAdmin);
        createAnnouncement("Export check");

        String csv = mockMvc.perform(get("/api/system-logs/export").header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("system-logs-")))
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).startsWith("Timestamp,Actor,Actor Email,Role,Module,Action,Outcome,Target,Description");
        assertThat(csv).contains("CREATE_ANNOUNCEMENT");
        // A value starting with = is prefixed with ' so spreadsheets show it as text instead of running it
        assertThat(csv).contains(",\"'=SUM(1+1)\",").doesNotContain(",\"=SUM(1+1)\",");
        assertThat(logs("EXPORT_SYSTEM_LOGS")).hasSize(1);
        assertThat(logs("EXPORT_SYSTEM_LOGS").get(0).getDescription()).startsWith("Exported 1 system log entries");

        mockMvc.perform(get("/api/system-logs/export").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Log entries cannot be edited or deleted through the API")
    void logsAreAppendOnly() throws Exception {
        createAnnouncement("Immutable");
        long logId = activityLogRepository.findAll().get(0).getId();

        int put = mockMvc.perform(put("/api/system-logs/" + logId).header("Authorization", bearer(masterAdmin))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus();
        int del = mockMvc.perform(delete("/api/system-logs/" + logId).header("Authorization", bearer(masterAdmin)))
                .andReturn().getResponse().getStatus();

        assertThat(put).isGreaterThanOrEqualTo(400);
        assertThat(del).isGreaterThanOrEqualTo(400);
        assertThat(activityLogRepository.existsById(logId)).isTrue();
    }

    @Test
    @DisplayName("Routine entries written before the policy (sign-ins, setup, residents' own edits) are hidden from the log")
    void legacyRoutineEntriesAreHidden() throws Exception {
        legacy("LOGIN", ActivityLog.OUTCOME_SUCCESS, "ADMIN");
        legacy("SETUP_PROFILE", ActivityLog.OUTCOME_SUCCESS, "RESIDENT");
        legacy("UPDATE_PROFILE", ActivityLog.OUTCOME_SUCCESS, "RESIDENT");
        legacy("LOGIN", ActivityLog.OUTCOME_DENIED, null);
        legacy("APPROVE_PAYMENT_SUBMISSION", ActivityLog.OUTCOME_SUCCESS, "ADMIN");
        legacy("UPDATE_PROFILE", ActivityLog.OUTCOME_SUCCESS, "ADMIN");

        String body = mockMvc.perform(get("/api/system-logs").param("size", "100").header("Authorization", bearer(masterAdmin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(body).get("content");
        List<String> shown = new java.util.ArrayList<>();
        content.forEach(n -> shown.add(n.get("action").asText() + "/" + n.get("outcome").asText()));
        assertThat(shown).containsExactlyInAnyOrder(
                "LOGIN/DENIED", "APPROVE_PAYMENT_SUBMISSION/SUCCESS", "UPDATE_PROFILE/SUCCESS");
    }

    private void legacy(String action, String outcome, String actorRole) {
        ActivityLog log = new ActivityLog(null, action, "TEST", "legacy " + action, null);
        log.setOutcome(outcome);
        log.setActorRole(actorRole);
        activityLogRepository.save(log);
    }
}
