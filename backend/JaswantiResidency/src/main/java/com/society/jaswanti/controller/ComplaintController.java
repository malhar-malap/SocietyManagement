package com.society.jaswanti.controller;

import com.society.jaswanti.dto.ComplaintDto;
import com.society.jaswanti.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #residentId == authentication.principal.id")
    public ResponseEntity<ComplaintDto> createComplaint(
            @RequestParam("residentId") Long residentId,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        return ResponseEntity.ok(complaintService.createComplaint(residentId, category, description, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<ComplaintDto>> getAllComplaints(org.springframework.security.core.Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN"))) {
            return ResponseEntity.ok(complaintService.getAllComplaints());
        } else {
            com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
            return ResponseEntity.ok(complaintService.getComplaintsByResident(user.getId()));
        }
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #residentId == authentication.principal.id")
    public ResponseEntity<List<ComplaintDto>> getComplaintsByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(complaintService.getComplaintsByResident(residentId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ComplaintDto> updateComplaint(@PathVariable Long id, @RequestBody com.society.jaswanti.entity.Complaint complaint) {
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
}
