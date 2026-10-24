package com.society.jaswanti.controller;

import com.society.jaswanti.dto.ComplaintDto;
import com.society.jaswanti.entity.Complaint;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.ComplaintService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    public ComplaintController(ComplaintService complaintService, UserRepository userRepository) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
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

    private boolean isAdminOrMaster(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MASTER_ADMIN".equals(a.getAuthority()));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<ComplaintDto> createComplaint(
            @RequestParam(value = "residentId", required = false) Long residentId,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam(value = "submissionType", defaultValue = "COMPLAINT") String submissionType,
            @RequestParam(value = "permissionCategory", required = false) String permissionCategory,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication auth) throws IOException {

        Long effectiveResidentId = residentId;
        if (!isAdminOrMaster(auth) || effectiveResidentId == null) {
            effectiveResidentId = getAuthenticatedUserId(auth);
        }
        if (effectiveResidentId == null) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(complaintService.createSubmission(
                effectiveResidentId, category, description, submissionType, permissionCategory, startDate, endDate, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints(Authentication auth) {
        if (isAdminOrMaster(auth)) {
            return ResponseEntity.ok(complaintService.getAllComplaints());
        } else {
            Long userId = getAuthenticatedUserId(auth);
            if (userId == null) return ResponseEntity.status(403).build();
            return ResponseEntity.ok(complaintService.getComplaintsByResident(userId));
        }
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<ComplaintDto>> getComplaintsByResident(@PathVariable Long residentId, Authentication auth) {
        if (!isAdminOrMaster(auth)) {
            Long userId = getAuthenticatedUserId(auth);
            if (userId == null || !userId.equals(residentId)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(complaintService.getComplaintsByResident(residentId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ComplaintDto> updateComplaint(@PathVariable Long id, @RequestBody Complaint complaint) {
        return complaintService.updateComplaint(id, complaint)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ComplaintDto> updateComplaintStatus(@PathVariable Long id, @RequestParam String status) {
        return complaintService.updateComplaintStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ComplaintDto> respondToSubmission(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String adminResponse = body.get("adminResponse");
        return complaintService.respondToSubmission(id, status, adminResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/attachment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Long id, Authentication auth) {
        Complaint complaint = complaintService.getComplaintEntity(id)
                .orElseThrow(() -> new com.society.jaswanti.exception.ResourceNotFoundException("Complaint not found with ID: " + id));

        if (!isAdminOrMaster(auth)) {
            Long userId = getAuthenticatedUserId(auth);
            if (userId == null || !complaint.getResident().getId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
        }

        if (complaint.getImageData() == null || complaint.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (complaint.getImageContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(complaint.getImageContentType());
            } catch (Exception ignored) {}
        }

        String filename = complaint.getImageFilename() != null ? complaint.getImageFilename() : "attachment_" + id;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(complaint.getImageData());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
}
