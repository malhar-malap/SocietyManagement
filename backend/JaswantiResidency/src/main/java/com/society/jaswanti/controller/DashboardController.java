package com.society.jaswanti.controller;

import com.society.jaswanti.dto.DashboardDto;
import com.society.jaswanti.service.AnnouncementService;
import com.society.jaswanti.service.ComplaintService;
import com.society.jaswanti.service.MaintenanceBillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resident/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final AnnouncementService announcementService;
    private final ComplaintService complaintService;
    private final MaintenanceBillService billService;

    public DashboardController(AnnouncementService announcementService, ComplaintService complaintService, MaintenanceBillService billService) {
        this.announcementService = announcementService;
        this.complaintService = complaintService;
        this.billService = billService;
    }

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<DashboardDto> getDashboardData(
            @RequestParam("residentId") Long residentId,
            @RequestParam("roomId") Long roomId) {
        
        DashboardDto dashboardDto = new DashboardDto(
                announcementService.getAllActiveAnnouncements(),
                complaintService.getComplaintsByResident(residentId),
                billService.getBillsByRoom(roomId)
        );
        return ResponseEntity.ok(dashboardDto);
    }
}
