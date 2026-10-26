package com.society.jaswanti.controller;

import com.society.jaswanti.dto.DashboardDto;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.AnnouncementService;
import com.society.jaswanti.service.ComplaintService;
import com.society.jaswanti.service.MaintenanceBillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resident/dashboard")
public class DashboardController {

    private final AnnouncementService announcementService;
    private final ComplaintService complaintService;
    private final MaintenanceBillService billService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;

    public DashboardController(AnnouncementService announcementService,
                               ComplaintService complaintService,
                               MaintenanceBillService billService,
                               UserRepository userRepository,
                               RoomRepository roomRepository,
                               RoomResidentRepository roomResidentRepository) {
        this.announcementService = announcementService;
        this.complaintService = complaintService;
        this.billService = billService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
    }

    private Long getAuthenticatedUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElse(null);
    }

    private List<Long> getAuthorizedRoomIdsForResident(Long userId) {
        List<Long> roomIds = roomResidentRepository.findByResidentIdAndIsActiveTrue(userId).stream()
                .map(rr -> rr.getRoom() != null ? rr.getRoom().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        List<Room> legacyRooms = roomRepository.findByResidentId(userId);
        for (Room r : legacyRooms) {
            if (r.getId() != null && !roomResidentRepository.existsByRoomId(r.getId()) && !roomIds.contains(r.getId())) {
                roomIds.add(r.getId());
            }
        }
        return roomIds;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<DashboardDto> getResidentDashboard(
            @RequestParam(value = "residentId", required = false) Long residentId,
            @RequestParam(value = "roomId", required = false) Long roomId,
            Authentication auth) {

        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return ResponseEntity.status(403).build();

        // Prevent resident from spoofing another resident ID
        if (residentId != null && !residentId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        List<Long> authorizedRooms = getAuthorizedRoomIdsForResident(userId);
        final Long targetRoomId = (roomId != null) ? roomId : (!authorizedRooms.isEmpty() ? authorizedRooms.get(0) : null);

        if (roomId != null && !authorizedRooms.contains(roomId)) {
            return ResponseEntity.status(403).build();
        }

        DashboardDto dashboardDto = new DashboardDto(
                announcementService.getAllActiveAnnouncements(),
                complaintService.getComplaintsByResident(userId),
                targetRoomId != null ? billService.getBillsByRoom(targetRoomId) : Collections.emptyList()
        );
        return ResponseEntity.ok(dashboardDto);
    }
}
