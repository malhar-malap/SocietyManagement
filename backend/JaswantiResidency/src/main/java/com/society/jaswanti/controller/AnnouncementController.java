package com.society.jaswanti.controller;

import com.society.jaswanti.dto.AnnouncementDto;
import com.society.jaswanti.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private com.society.jaswanti.repository.UserRepository userRepository;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<AnnouncementDto>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllActiveAnnouncements());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<AnnouncementDto>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<AnnouncementDto> createAnnouncement(
            @RequestBody com.society.jaswanti.entity.Announcement announcement,
            @RequestParam(required = false) Long adminId,
            org.springframework.security.core.Authentication auth) {
        Long effectiveAdminId = adminId;
        if (effectiveAdminId == null && auth != null) {
            effectiveAdminId = userRepository.findByEmail(auth.getName()).map(com.society.jaswanti.entity.User::getId).orElse(null);
        }
        return ResponseEntity.ok(announcementService.createAnnouncement(announcement, effectiveAdminId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<AnnouncementDto> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody com.society.jaswanti.entity.Announcement announcement) {
        return announcementService.updateAnnouncement(id, announcement)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
}
