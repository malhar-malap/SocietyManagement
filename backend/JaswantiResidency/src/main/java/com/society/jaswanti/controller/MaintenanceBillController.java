package com.society.jaswanti.controller;

import com.society.jaswanti.dto.MaintenanceBillDto;
import com.society.jaswanti.entity.ChargeType;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.MaintenanceBillService;
import com.society.jaswanti.service.PdfInvoiceGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class MaintenanceBillController {

    private final MaintenanceBillService billService;
    private final com.society.jaswanti.service.PaymentRecordingService paymentRecordingService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;

    public MaintenanceBillController(MaintenanceBillService billService,
                                     com.society.jaswanti.service.PaymentRecordingService paymentRecordingService,
                                     RoomRepository roomRepository,
                                     UserRepository userRepository,
                                     RoomResidentRepository roomResidentRepository,
                                     PdfInvoiceGenerator pdfInvoiceGenerator) {
        this.billService = billService;
        this.paymentRecordingService = paymentRecordingService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.pdfInvoiceGenerator = pdfInvoiceGenerator;
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

    private boolean isAuthorizedForBill(Authentication auth, Long billId) {
        if (auth == null || billId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN"))) {
            return true;
        }
        return billService.getBillById(billId)
                .map(bill -> isAuthorizedForRoom(auth, bill.getRoomId()))
                .orElse(false);
    }

    private boolean isAuthorizedForRoom(Authentication auth, Long roomId) {
        if (auth == null || roomId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN"))) {
            return true;
        }
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return false;

        // Primary: active RoomResident relationship
        if (roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(roomId, userId)) {
            return true;
        }

        // Safe legacy fallback only if no relationships have ever been recorded for this room
        if (!roomResidentRepository.existsByRoomId(roomId)) {
            return roomRepository.findById(roomId)
                    .map(r -> r.getResident() != null && r.getResident().getId().equals(userId))
                    .orElse(false);
        }

        return false;
    }

    private List<Long> getAuthorizedRoomIdsForResident(Long userId) {
        List<Long> roomIds = roomResidentRepository.findByResidentIdAndIsActiveTrue(userId).stream()
                .map(rr -> rr.getRoom() != null ? rr.getRoom().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        // Legacy fallback only for rooms with zero relationship records
        List<Room> legacyRooms = roomRepository.findByResidentId(userId);
        for (Room r : legacyRooms) {
            if (r.getId() != null && !roomResidentRepository.existsByRoomId(r.getId()) && !roomIds.contains(r.getId())) {
                roomIds.add(r.getId());
            }
        }
        return roomIds;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<MaintenanceBillDto> createBill(@RequestBody MaintenanceBill bill) {
        return ResponseEntity.ok(billService.createBill(bill));
    }

    @PostMapping("/generate-monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<MaintenanceBillDto>> generateMonthlyBills(
            @RequestParam String billingMonth,
            @RequestParam BigDecimal amount,
            @RequestParam String dueDate,
            @RequestParam(required = false, defaultValue = "MAINTENANCE") ChargeType chargeType) {
        return ResponseEntity.ok(billService.generateMonthlyBills(billingMonth, amount, LocalDate.parse(dueDate), chargeType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<MaintenanceBillDto> updateBill(@PathVariable Long id, @RequestBody MaintenanceBill bill) {
        return billService.updateBill(id, bill)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<MaintenanceBillDto> getBill(@PathVariable Long id, Authentication auth) {
        if (!isAuthorizedForBill(auth, id)) return ResponseEntity.status(403).build();
        return billService.getBillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id, Authentication auth) {
        if (!isAuthorizedForBill(auth, id)) return ResponseEntity.status(403).build();

        MaintenanceBill bill = billService.getBillEntityById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        byte[] pdfContent = pdfInvoiceGenerator.generateInvoice(bill);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + id + ".pdf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfContent);
    }

    @Deprecated
    @PutMapping("/{id}/mark-paid-cash")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<MaintenanceBillDto> markPaidCash(@PathVariable Long id, Authentication auth) {
        return billService.markPaidCash(id, auth)
                .map(dto -> ResponseEntity.ok()
                        .header("X-Deprecated", "PUT /api/bills/{id}/mark-paid-cash is deprecated; use POST /api/bills/{id}/record-payment instead")
                        .header("Warning", "299 - \"PUT /api/bills/{id}/mark-paid-cash is deprecated; use POST /api/bills/{id}/record-payment instead\"")
                        .body(dto))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{billId}/record-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<com.society.jaswanti.dto.RecordPaymentResponse> recordPayment(
            @PathVariable Long billId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @jakarta.validation.Valid @RequestBody com.society.jaswanti.dto.RecordPaymentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(paymentRecordingService.recordPayment(billId, idempotencyKeyHeader, request, auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<MaintenanceBillDto>> getAllBills() {
        return ResponseEntity.ok(billService.getAllBills());
    }

    @GetMapping("/my-bills")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<MaintenanceBillDto>> getMyBills(Authentication auth) {
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return ResponseEntity.status(403).build();

        List<Long> authorizedRoomIds = getAuthorizedRoomIdsForResident(userId);
        List<MaintenanceBillDto> myBills = authorizedRoomIds.stream()
                .flatMap(roomId -> billService.getBillsByRoom(roomId).stream())
                .collect(Collectors.toList());
        return ResponseEntity.ok(myBills);
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<MaintenanceBillDto>> getBillsByRoom(@PathVariable Long roomId, Authentication auth) {
        if (!isAuthorizedForRoom(auth, roomId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(billService.getBillsByRoom(roomId));
    }
}
