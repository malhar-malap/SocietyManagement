package com.society.jaswanti.controller;

import com.society.jaswanti.dto.MaintenanceBillDto;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.service.MaintenanceBillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class MaintenanceBillController {

    private final MaintenanceBillService billService;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final com.society.jaswanti.service.PdfInvoiceGenerator pdfInvoiceGenerator;

    public MaintenanceBillController(MaintenanceBillService billService, 
                                     com.society.jaswanti.repository.RoomRepository roomRepository,
                                     com.society.jaswanti.service.PdfInvoiceGenerator pdfInvoiceGenerator) {
        this.billService = billService;
        this.roomRepository = roomRepository;
        this.pdfInvoiceGenerator = pdfInvoiceGenerator;
    }

    private boolean isAuthorizedForBill(org.springframework.security.core.Authentication auth, Long billId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        return billService.getBillById(billId).map(bill -> {
            java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(user.getId());
            return rooms.stream().anyMatch(r -> r.getId().equals(bill.getRoomId()));
        }).orElse(false);
    }

    private boolean isAuthorizedForRoom(org.springframework.security.core.Authentication auth, Long roomId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(user.getId());
        return rooms.stream().anyMatch(r -> r.getId().equals(roomId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceBillDto> createBill(@RequestBody MaintenanceBill bill) {
        return ResponseEntity.ok(billService.createBill(bill));
    }

    @PostMapping("/generate-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MaintenanceBillDto>> generateMonthlyBills(
            @RequestParam String billingMonth,
            @RequestParam Double amount,
            @RequestParam String dueDate) {
        return ResponseEntity.ok(billService.generateMonthlyBills(billingMonth, amount, java.time.LocalDate.parse(dueDate)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceBillDto> updateBill(@PathVariable Long id, @RequestBody MaintenanceBill bill) {
        return billService.updateBill(id, bill)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<MaintenanceBillDto> getBill(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        if (!isAuthorizedForBill(auth, id)) return ResponseEntity.status(403).build();
        return billService.getBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        if (!isAuthorizedForBill(auth, id)) return ResponseEntity.status(403).build();
        
        com.society.jaswanti.entity.MaintenanceBill bill = billService.getBillEntityById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
                
        byte[] pdfContent = pdfInvoiceGenerator.generateInvoice(bill);
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + id + ".pdf\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfContent);
    }

    @PutMapping("/{id}/mark-paid-cash")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceBillDto> markPaidCash(@PathVariable Long id) {
        return billService.markPaidCash(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<MaintenanceBillDto>> getAllBills() {
        return ResponseEntity.ok(billService.getAllBills());
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<MaintenanceBillDto>> getBillsByRoom(@PathVariable Long roomId, org.springframework.security.core.Authentication auth) {
        if (!isAuthorizedForRoom(auth, roomId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(billService.getBillsByRoom(roomId));
    }
}
