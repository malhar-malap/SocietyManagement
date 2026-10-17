package com.society.jaswanti.controller;

import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.ComplaintStatus;
import com.society.jaswanti.repository.AnnouncementRepository;
import com.society.jaswanti.repository.ComplaintRepository;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final MaintenanceBillRepository billRepository;
    private final ComplaintRepository complaintRepository;
    private final AnnouncementRepository announcementRepository;

    public AdminDashboardController(UserRepository userRepository, MaintenanceBillRepository billRepository,
                                    ComplaintRepository complaintRepository, AnnouncementRepository announcementRepository) {
        this.userRepository = userRepository;
        this.billRepository = billRepository;
        this.complaintRepository = complaintRepository;
        this.announcementRepository = announcementRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalResidents = userRepository.count();
        stats.put("totalResidents", totalResidents);
        
        Double monthlyCollection = billRepository.findAll().stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .mapToDouble(b -> b.getAmount())
                .sum();
        stats.put("monthlyCollection", monthlyCollection);
        
        Double pendingAmount = billRepository.findAll().stream()
                .filter(b -> b.getStatus() != BillStatus.PAID)
                .mapToDouble(b -> b.getAmount() + (b.getLateFee() != null ? b.getLateFee() : 0.0))
                .sum();
        stats.put("pendingAmount", pendingAmount);
        
        long openComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        stats.put("openComplaints", openComplaints);
        
        long unpaidResidents = billRepository.findAll().stream()
                .filter(b -> b.getStatus() != BillStatus.PAID)
                .map(b -> b.getRoom().getResident() != null ? b.getRoom().getResident().getId() : -1L)
                .filter(id -> id != -1L)
                .distinct()
                .count();
        stats.put("unpaidResidents", unpaidResidents);
        
        long activeAnnouncements = announcementRepository.findAll().size(); // Or filter active ones
        stats.put("activeAnnouncements", activeAnnouncements);
        
        return ResponseEntity.ok(stats);
    }
}
