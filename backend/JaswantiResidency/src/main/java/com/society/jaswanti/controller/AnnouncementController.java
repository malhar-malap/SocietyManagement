package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.audit.AuditedAction;
import com.society.jaswanti.audit.AuditTargetResolver;

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
    @AuditedAction(action = "CREATE_ANNOUNCEMENT", module = "ANNOUNCEMENTS", summary = "Posted an announcement", target = AuditTargetResolver.ANNOUNCEMENT)
    public ResponseEntity<AnnouncementDto> createAnnouncement(
            @RequestBody com.society.jaswanti.entity.Announcement announcement,
            @RequestParam(required = false) Long adminId,
            org.springframework.security.core.Authentication auth) {
        Long effectiveAdminId = adminId;
        if (effectiveAdminId == null && auth != null) {
            effectiveAdminId = userRepository.findByEmail(auth.getName()).map(com.society.jaswanti.entity.User::getId).orElse(null);
        }
        AnnouncementDto created = announcementService.createAnnouncement(announcement, effectiveAdminId);
        AuditTrail.target(AuditTargetResolver.ANNOUNCEMENT, created.getId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @AuditedAction(action = "UPDATE_ANNOUNCEMENT", module = "ANNOUNCEMENTS", summary = "Edited an announcement", target = AuditTargetResolver.ANNOUNCEMENT)
    public ResponseEntity<AnnouncementDto> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody com.society.jaswanti.entity.Announcement announcement) {
        return announcementService.updateAnnouncement(id, announcement)
                .map(updated -> {
                    AuditTrail.describe("Edited an announcement; title is now \"" + updated.getTitle() + "\"");
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @AuditedAction(action = "DELETE_ANNOUNCEMENT", module = "ANNOUNCEMENTS", summary = "Deleted an announcement", target = AuditTargetResolver.ANNOUNCEMENT)
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }
}
