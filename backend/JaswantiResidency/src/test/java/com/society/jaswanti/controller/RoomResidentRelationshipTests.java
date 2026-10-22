package com.society.jaswanti.controller;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import com.society.jaswanti.security.CustomUserDetailsService;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.society.jaswanti.dto.CorrectOccupancyDatesRequest;
import com.society.jaswanti.service.ActivityLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoomResidentRelationshipTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomResidentRepository roomResidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceBillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @MockitoSpyBean
    private ActivityLogService activityLogService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private User admin;
    private User residentA;
    private User residentB;
    private User residentC;

    private Room room1;
    private Room room2;

    @BeforeEach
    void setUp() {
        doCallRealMethod().when(activityLogService).logActivity(any(), any(), any(), any());

        admin = findOrCreateUser("rel-test-admin@jaswanti.com", "Rel Admin", Role.ADMIN);
        residentA = findOrCreateUser("rel-test-a@jaswanti.com", "Resident A", Role.RESIDENT);
        residentB = findOrCreateUser("rel-test-b@jaswanti.com", "Resident B", Role.RESIDENT);
        residentC = findOrCreateUser("rel-test-c@jaswanti.com", "Resident C", Role.RESIDENT);

        room1 = findOrCreateRoom("R-901", 9);
        room2 = findOrCreateRoom("R-902", 9);

        // Clear existing room_residents for test rooms to ensure isolation
        roomResidentRepository.findByRoomId(room1.getId()).forEach(roomResidentRepository::delete);
        roomResidentRepository.findByRoomId(room2.getId()).forEach(roomResidentRepository::delete);

        // Clear rooms.resident
        room1.setResident(null);
        roomRepository.save(room1);
        room2.setResident(null);
        roomRepository.save(room2);
    }

    @Test
    void migrationScriptsDefineTableAndDocumentBackfillLimitation() throws Exception {
        String migrationSql = new String(new ClassPathResource(
                "db/migration/V5__create_unit_resident_relationships.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(migrationSql)
                .contains("CREATE TABLE room_residents")
                .contains("relationship_type ENUM('OWNER', 'TENANT')")
                .contains("start_date")
                .contains("end_date")
                .contains("is_active")
                .contains("is_primary_contact")
                .contains("INSERT INTO room_residents");

        String v6Sql = new String(new ClassPathResource(
                "db/migration/V6__correct_room_residents_backfill_start_date.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(v6Sql)
                .contains("Document legacy occupancy backfill limitation")
                .contains("SELECT 1;");
    }

    @Test
    void residentWithMultipleUnitsCanAccessAllTheirRoomsAndLedgers() throws Exception {
        RoomResident rel1 = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.now().minusMonths(6), true);
        RoomResident rel2 = new RoomResident(room2, residentA, RelationshipType.TENANT, LocalDate.now().minusMonths(2), true);
        roomResidentRepository.save(rel1);
        roomResidentRepository.save(rel2);

        MaintenanceBill bill1 = createBill(room1, 3500.0, "2026-06");
        MaintenanceBill bill2 = createBill(room2, 4200.0, "2026-06");

        String token = jwtService.generateToken(residentA);

        mockMvc.perform(get("/api/rooms/my-rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].roomNumber", containsInAnyOrder("R-901", "R-902")));

        mockMvc.perform(get("/api/bills/my-bills")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(bill1.getId().intValue(), bill2.getId().intValue())));
    }

    @Test
    void residentVsAdminOccupantVisibilityEnforcesPrivacy() throws Exception {
        // Room 1 has active Owner (A), active Tenant (B), and former ended Tenant (C)
        RoomResident relA = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.now().minusMonths(12), true);
        RoomResident relB = new RoomResident(room1, residentB, RelationshipType.TENANT, LocalDate.now().minusMonths(1), false);
        RoomResident relFormer = new RoomResident(room1, residentC, RelationshipType.TENANT, LocalDate.now().minusMonths(24), false);
        relFormer.setActive(false);
        relFormer.setEndDate(LocalDate.now().minusMonths(13));

        roomResidentRepository.save(relA);
        roomResidentRepository.save(relB);
        roomResidentRepository.save(relFormer);

        String tokenA = jwtService.generateToken(residentA);
        String tokenB = jwtService.generateToken(residentB);
        String tokenC = jwtService.generateToken(residentC);
        String tokenAdmin = jwtService.generateToken(admin);

        // Resident A can view occupants: only sees active occupants (A and B), NOT former occupant C
        mockMvc.perform(get("/api/rooms/{id}/occupants", room1.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].residentEmail", containsInAnyOrder("rel-test-a@jaswanti.com", "rel-test-b@jaswanti.com")))
                .andExpect(jsonPath("$[*].residentEmail", not(hasItem("rel-test-c@jaswanti.com"))))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true))
                .andExpect(jsonPath("$[0].endDate").doesNotExist())
                .andExpect(jsonPath("$[1].endDate").doesNotExist());

        // Resident B can also view active occupants only
        mockMvc.perform(get("/api/rooms/{id}/occupants", room1.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].residentEmail", not(hasItem("rel-test-c@jaswanti.com"))));

        // Former resident C cannot view occupants of room 1 (403 Forbidden)
        mockMvc.perform(get("/api/rooms/{id}/occupants", room1.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        // Admin can view full occupant history (both active and inactive)
        mockMvc.perform(get("/api/rooms/{id}/occupants", room1.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].residentEmail", containsInAnyOrder(
                        "rel-test-a@jaswanti.com", "rel-test-b@jaswanti.com", "rel-test-c@jaswanti.com")))
                .andExpect(jsonPath("$[?(@.residentEmail == 'rel-test-c@jaswanti.com')].active").value(hasItem(false)))
                .andExpect(jsonPath("$[?(@.residentEmail == 'rel-test-c@jaswanti.com')].endDate").value(hasItem(notNullValue())));
    }

    @Test
    void rejectDuplicateActiveOccupantAssignment() throws Exception {
        RoomResident relA = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.now().minusMonths(6), true);
        roomResidentRepository.save(relA);

        String tokenAdmin = jwtService.generateToken(admin);

        String duplicatePayload = "{" +
                "\"residentId\":" + residentA.getId() + "," +
                "\"relationshipType\":\"TENANT\"," +
                "\"startDate\":\"" + LocalDate.now() + "\"," +
                "\"primaryContact\":false" +
                "}";

        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Resident is already an active occupant of this room"));
    }

    @Test
    void rejectAssignmentOfNonResidentOrInactiveUser() throws Exception {
        String tokenAdmin = jwtService.generateToken(admin);

        // Attempt to assign an ADMIN
        String nonResidentPayload = "{" +
                "\"residentId\":" + admin.getId() + "," +
                "\"relationshipType\":\"OWNER\"," +
                "\"startDate\":\"" + LocalDate.now() + "\"," +
                "\"primaryContact\":true" +
                "}";

        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonResidentPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only users with role RESIDENT can be assigned as room occupants"));

        // Attempt to assign a blocked/inactive resident
        User blockedResident = findOrCreateUser("rel-blocked@jaswanti.com", "Blocked Resident", Role.RESIDENT);
        blockedResident.setActive(false);
        blockedResident.setStatus(UserStatus.BLOCKED);
        userRepository.save(blockedResident);

        String inactivePayload = "{" +
                "\"residentId\":" + blockedResident.getId() + "," +
                "\"relationshipType\":\"TENANT\"," +
                "\"startDate\":\"" + LocalDate.now() + "\"," +
                "\"primaryContact\":false" +
                "}";

        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactivePayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot assign an inactive or deactivated user as a room occupant"));
    }

    @Test
    void occupancyLifecycleDateValidations() throws Exception {
        String tokenAdmin = jwtService.generateToken(admin);

        // 1. Cannot specify end date for active assignment
        String payloadWithEndDate = "{" +
                "\"residentId\":" + residentB.getId() + "," +
                "\"relationshipType\":\"TENANT\"," +
                "\"startDate\":\"" + LocalDate.now() + "\"," +
                "\"endDate\":\"" + LocalDate.now().plusMonths(6) + "\"," +
                "\"primaryContact\":false" +
                "}";

        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithEndDate)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An active relationship must not have an end date"));

        // Future start date is rejected on assignment
        String futureStartPayload = "{" +
                "\"residentId\":" + residentB.getId() + "," +
                "\"relationshipType\":\"TENANT\"," +
                "\"startDate\":\"" + LocalDate.now().plusDays(1) + "\"," +
                "\"primaryContact\":false" +
                "}";

        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureStartPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be in the future"));

        // 2. Cannot end occupancy with end date before start date
        RoomResident rel = new RoomResident(room1, residentB, RelationshipType.TENANT, LocalDate.of(2026, 6, 1), false);
        rel = roomResidentRepository.save(rel);

        mockMvc.perform(put("/api/rooms/occupants/{id}/end", rel.getId())
                        .param("endDate", "2026-05-15")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date cannot precede start date"));

        // End properly
        mockMvc.perform(put("/api/rooms/occupants/{id}/end", rel.getId())
                        .param("endDate", "2026-06-15")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // 3. Ending an already inactive relationship returns clear client error
        mockMvc.perform(put("/api/rooms/occupants/{id}/end", rel.getId())
                        .param("endDate", "2026-06-20")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Occupancy relationship is already inactive"));
    }

    @Test
    void endedOccupantCannotRegainRoomAccessOrAppearThroughStaleRoomResidentData() throws Exception {
        // Set stale room.resident to residentB
        room1.setResident(residentB);
        roomRepository.save(room1);

        // But roomResident has ONLY an ended relationship for residentB
        RoomResident endedRel = new RoomResident(room1, residentB, RelationshipType.TENANT, LocalDate.now().minusMonths(10), false);
        endedRel.setActive(false);
        endedRel.setEndDate(LocalDate.now().minusDays(5));
        roomResidentRepository.save(endedRel);

        String tokenB = jwtService.generateToken(residentB);

        // Resident B has no access to room 1 via /my-rooms
        mockMvc.perform(get("/api/rooms/my-rooms")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Resident B cannot view room 1 occupants
        mockMvc.perform(get("/api/rooms/{id}/occupants", room1.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Resident B cannot view bills for room 1
        mockMvc.perform(get("/api/bills/room/{id}", room1.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Stale room.resident is NOT resolved as current resident name or ID
        assertThat(roomService.resolveResidentName(room1)).isNull();
        assertThat(roomService.resolveResidentId(room1)).isNull();

        // Room number login fails because no active occupant exists
        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("R-901"));
    }

    @Test
    void legacyFallbackWorksWhenNoRelationshipHistoryExists() throws Exception {
        // Room 2 has legacy residentA assigned, and ZERO rows in room_residents
        room2.setResident(residentA);
        roomRepository.save(room2);

        assertThat(roomResidentRepository.existsByRoomId(room2.getId())).isFalse();

        // Fallback resolves to legacy resident
        assertThat(roomService.resolveResidentName(room2)).isEqualTo("Resident A");
        assertThat(roomService.resolveResidentId(room2)).isEqualTo(residentA.getId());

        String tokenA = jwtService.generateToken(residentA);

        // Resident A can access room 2 via /my-rooms
        mockMvc.perform(get("/api/rooms/my-rooms")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].roomNumber", hasItem("R-902")));
    }

    @Test
    void adminCanAssignAndEndOccupancyWhileResidentIsForbidden() throws Exception {
        String tokenAdmin = jwtService.generateToken(admin);
        String tokenResident = jwtService.generateToken(residentA);

        String assignPayload = "{" +
                "\"residentId\":" + residentB.getId() + "," +
                "\"relationshipType\":\"TENANT\"," +
                "\"startDate\":\"" + LocalDate.now() + "\"," +
                "\"primaryContact\":true" +
                "}";

        // Resident cannot assign occupants
        mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload)
                        .header("Authorization", "Bearer " + tokenResident))
                .andExpect(status().isForbidden());

        // Admin can assign occupant
        String assignResponse = mockMvc.perform(post("/api/rooms/{id}/occupants", room1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentEmail").value("rel-test-b@jaswanti.com"))
                .andExpect(jsonPath("$.relationshipType").value("TENANT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.primaryContact").value(true))
                .andReturn().getResponse().getContentAsString();

        Number parsedId = com.jayway.jsonpath.JsonPath.read(assignResponse, "$.id");
        Long occupantId = parsedId.longValue();

        // Resident cannot end occupancy
        mockMvc.perform(put("/api/rooms/occupants/{id}/end", occupantId)
                        .header("Authorization", "Bearer " + tokenResident))
                .andExpect(status().isForbidden());

        // Admin can end occupancy
        mockMvc.perform(put("/api/rooms/occupants/{id}/end", occupantId)
                        .param("endDate", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.primaryContact").value(false));

        // Verify in database
        RoomResident persisted = roomResidentRepository.findById(occupantId).orElseThrow();
        assertThat(persisted.isActive()).isFalse();
        assertThat(persisted.getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void adminCanCorrectOccupancyDatesWithAuditLog() throws Exception {
        RoomResident rel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        rel = roomResidentRepository.save(rel);

        String tokenAdmin = jwtService.generateToken(admin);
        String correctPayload = "{" +
                "\"startDate\":\"2024-05-15\"," +
                "\"reason\":\"Verified move-in date from physical lease registry\"" +
                "}";

        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", rel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2024-05-15"))
                .andExpect(jsonPath("$.active").value(true));

        RoomResident updated = roomResidentRepository.findById(rel.getId()).orElseThrow();
        assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2024, 5, 15));

        // Verify audit log entry was created
        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).anyMatch(log ->
                "OCCUPANCY_DATE_CORRECTION".equals(log.getAction()) &&
                "REGISTRY".equals(log.getModule()) &&
                log.getDescription().contains("2026-01-01 -> 2024-05-15") &&
                log.getDescription().contains("Verified move-in date from physical lease registry") &&
                log.getUser() != null && log.getUser().getId().equals(admin.getId())
        );
    }

    @Test
    void adminCanCorrectLegacyDateToNullIfHistoricallyUnknown() throws Exception {
        RoomResident rel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        rel = roomResidentRepository.save(rel);

        String tokenAdmin = jwtService.generateToken(admin);
        String correctPayload = "{" +
                "\"startDate\":null," +
                "\"reason\":\"Move-in date unknown in pre-digitization paper records\"" +
                "}";

        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", rel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").isEmpty())
                .andExpect(jsonPath("$.active").value(true));

        RoomResident updated = roomResidentRepository.findById(rel.getId()).orElseThrow();
        assertThat(updated.getStartDate()).isNull();

        List<ActivityLog> logs = activityLogRepository.findAll();
        assertThat(logs).anyMatch(log ->
                "OCCUPANCY_DATE_CORRECTION".equals(log.getAction()) &&
                log.getDescription().contains("2026-01-01 -> null") &&
                log.getDescription().contains("Move-in date unknown in pre-digitization paper records")
        );
    }

    @Test
    void residentCannotCorrectOccupancyDates() throws Exception {
        RoomResident rel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        rel = roomResidentRepository.save(rel);

        String tokenResident = jwtService.generateToken(residentA);
        String correctPayload = "{" +
                "\"startDate\":\"2024-05-15\"," +
                "\"reason\":\"Self-reported change\"" +
                "}";

        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", rel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload)
                        .header("Authorization", "Bearer " + tokenResident))
                .andExpect(status().isForbidden());
    }

    @Test
    void dateCorrectionValidatesMandatoryReasonAndDateConsistency() throws Exception {
        RoomResident activeRel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        activeRel = roomResidentRepository.save(activeRel);

        RoomResident inactiveRel = new RoomResident(room1, residentB, RelationshipType.TENANT, LocalDate.of(2024, 1, 1), false);
        inactiveRel.setActive(false);
        inactiveRel.setEndDate(LocalDate.of(2024, 12, 31));
        inactiveRel = roomResidentRepository.save(inactiveRel);

        String tokenAdmin = jwtService.generateToken(admin);

        // Missing/blank reason
        String blankReasonPayload = "{\"startDate\":\"2024-05-15\",\"reason\":\"   \"}";
        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", activeRel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankReasonPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Correction reason is required and cannot be blank")));

        // Active occupant cannot have endDate set via date correction
        String activeWithEndPayload = "{\"startDate\":\"2024-05-15\",\"endDate\":\"2025-01-01\",\"reason\":\"Valid reason\"}";
        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", activeRel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeWithEndPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot set an end date on an active occupancy record")));

        // Inactive occupant cannot have endDate precede startDate
        String invalidDatesPayload = "{\"startDate\":\"2025-01-01\",\"endDate\":\"2024-01-01\",\"reason\":\"Valid reason\"}";
        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", inactiveRel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDatesPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("End date cannot precede start date")));

        // Future start date is rejected on date correction
        String futureCorrectionPayload = "{\"startDate\":\"" + LocalDate.now().plusDays(5) + "\",\"reason\":\"Future move-in correction\"}";
        mockMvc.perform(put("/api/rooms/occupants/{id}/correct-dates", activeRel.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureCorrectionPayload)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Start date cannot be in the future")));
    }

    @Test
    void dateCorrectionFailsAndRollsBackWhenActorCannotBeResolved() {
        RoomResident rel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        RoomResident savedRel = roomResidentRepository.save(rel);

        CorrectOccupancyDatesRequest request = new CorrectOccupancyDatesRequest(
                LocalDate.of(2024, 5, 15),
                null,
                "Valid verification reason"
        );

        UsernamePasswordAuthenticationToken unresolvableAuth =
                new UsernamePasswordAuthenticationToken("ghost-actor@jaswanti.com", "password");

        assertThrows(AccessDeniedException.class, () ->
                roomService.correctOccupancyDates(savedRel.getId(), request, unresolvableAuth)
        );

        // Confirm rollback: start_date is unchanged in database
        RoomResident persisted = roomResidentRepository.findById(savedRel.getId()).orElseThrow();
        assertThat(persisted.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void dateCorrectionFailsAndRollsBackWhenAuditPersistenceFails() {
        RoomResident rel = new RoomResident(room1, residentA, RelationshipType.OWNER, LocalDate.of(2026, 1, 1), true);
        RoomResident savedRel = roomResidentRepository.save(rel);

        CorrectOccupancyDatesRequest request = new CorrectOccupancyDatesRequest(
                LocalDate.of(2024, 5, 15),
                null,
                "Valid verification reason"
        );

        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(admin.getEmail(), "password");

        doThrow(new RuntimeException("Simulated audit write failure"))
                .when(activityLogService).logActivity(any(), any(), any(), any());

        assertThrows(RuntimeException.class, () ->
                roomService.correctOccupancyDates(savedRel.getId(), request, adminAuth)
        );

        // Confirm rollback: start_date is unchanged in database
        RoomResident persisted = roomResidentRepository.findById(savedRel.getId()).orElseThrow();
        assertThat(persisted.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    private User findOrCreateUser(String email, String name, Role role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName(name);
        user.setPassword("password123");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Room findOrCreateRoom(String roomNumber, int floor) {
        return roomRepository.findByRoomNumber(roomNumber).orElseGet(() -> {
            Room r = new Room(roomNumber, "C", floor);
            return roomRepository.save(r);
        });
    }

    private MaintenanceBill createBill(Room room, Double amount, String month) {
        MaintenanceBill bill = new MaintenanceBill();
        bill.setRoom(room);
        bill.setAmount(amount);
        bill.setBillingMonth(month);
        bill.setDueDate(LocalDate.now().plusDays(10));
        bill.setStatus(BillStatus.PENDING);
        return billRepository.save(bill);
    }
}
