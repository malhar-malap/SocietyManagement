package com.society.jaswanti.controller;

import com.society.jaswanti.dto.AssignResidentRequest;
import com.society.jaswanti.dto.CorrectOccupancyDatesRequest;
import com.society.jaswanti.dto.RoomResidentDto;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomService roomService;
    private final UserRepository userRepository;
    private final RoomResidentRepository roomResidentRepository;

    public RoomController(RoomService roomService,
                          UserRepository userRepository,
                          RoomResidentRepository roomResidentRepository) {
        this.roomService = roomService;
        this.userRepository = userRepository;
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
        return null;
    }

    private boolean isAdminOrMaster(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MASTER_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomService.saveRoom(room));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/my-rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<Room>> getMyRooms(Authentication auth) {
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(roomService.getRoomsByResidentId(userId));
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<Room>> getRoomsByResident(@PathVariable Long residentId, Authentication auth) {
        if (!isAdminOrMaster(auth)) {
            Long userId = getAuthenticatedUserId(auth);
            if (userId == null || !userId.equals(residentId)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(roomService.getRoomsByResidentId(residentId));
    }

    @GetMapping("/{id}/occupants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<RoomResidentDto>> getRoomOccupants(@PathVariable Long id, Authentication auth) {
        if (!isAdminOrMaster(auth)) {
            Long userId = getAuthenticatedUserId(auth);
            if (userId == null || !roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(id, userId)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(roomService.getActiveRoomOccupantsForResident(id));
        }
        return ResponseEntity.ok(roomService.getRoomOccupants(id));
    }

    @PostMapping("/{id}/occupants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<RoomResidentDto> assignOccupant(@PathVariable Long id, @RequestBody AssignResidentRequest request) {
        return ResponseEntity.ok(roomService.assignResidentToRoom(id, request));
    }

    @PutMapping("/occupants/{occupantId}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<RoomResidentDto> endOccupancy(
            @PathVariable Long occupantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(roomService.endOccupancy(occupantId, endDate));
    }

    @PutMapping("/occupants/{occupantId}/correct-dates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<RoomResidentDto> correctOccupancyDates(
            @PathVariable Long occupantId,
            @RequestBody CorrectOccupancyDatesRequest request,
            Authentication auth) {
        return ResponseEntity.ok(roomService.correctOccupancyDates(occupantId, request, auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room, Authentication auth) {
        return ResponseEntity.ok(roomService.updateRoom(id, room, auth));
    }

    @PostMapping(value = "/{id}/share-certificate", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<com.society.jaswanti.dto.ShareCertificateDto> uploadShareCertificate(
            @PathVariable Long id,
            @RequestParam(value = "certificateNumber", required = false) String certificateNumber,
            @RequestParam(value = "issuedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate,
            @RequestPart(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            Authentication auth) throws java.io.IOException {
        return ResponseEntity.ok(roomService.uploadShareCertificate(id, file, certificateNumber, issuedDate, auth));
    }

    @GetMapping("/{id}/share-certificate")
    @PreAuthorize("hasAnyRole('MASTER_ADMIN', 'ADMIN', 'RESIDENT')")
    public ResponseEntity<com.society.jaswanti.dto.ShareCertificateDto> getShareCertificate(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(roomService.getShareCertificateDto(id, auth));
    }

    @GetMapping("/{id}/share-certificate/download")
    @PreAuthorize("hasAnyRole('MASTER_ADMIN', 'ADMIN', 'RESIDENT')")
    public ResponseEntity<byte[]> downloadShareCertificate(
            @PathVariable Long id,
            Authentication auth) {
        Room room = roomService.getAuthorizedRoomForCertificate(id, auth);
        if (room.getShareCertificateData() == null || room.getShareCertificateData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        if (room.getShareCertificateContentType() != null) {
            try {
                mediaType = org.springframework.http.MediaType.parseMediaType(room.getShareCertificateContentType());
            } catch (Exception ignored) {}
        }

        String filename = room.getShareCertificateFilename() != null ? room.getShareCertificateFilename() : "share_certificate_" + room.getRoomNumber() + ".pdf";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(room.getShareCertificateData());
    }
}
