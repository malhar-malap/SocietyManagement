package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.audit.AuditedAction;
import com.society.jaswanti.audit.AuditTargetResolver;

import com.society.jaswanti.dto.*;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.MaintenanceBillService;
import com.society.jaswanti.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final MaintenanceBillService billService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final com.society.jaswanti.service.PdfInvoiceGenerator pdfInvoiceGenerator;
    private final com.society.jaswanti.service.EmailService emailService;

    public TransactionController(TransactionService transactionService,
            MaintenanceBillService billService,
            RoomRepository roomRepository,
            UserRepository userRepository,
            RoomResidentRepository roomResidentRepository,
            com.society.jaswanti.service.PdfInvoiceGenerator pdfInvoiceGenerator,
            com.society.jaswanti.service.EmailService emailService) {
        this.transactionService = transactionService;
        this.billService = billService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.pdfInvoiceGenerator = pdfInvoiceGenerator;
        this.emailService = emailService;
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

    private boolean isAuthorizedForRoom(Authentication auth, Long roomId) {
        if (auth == null || roomId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN") || a.getAuthority().equals("ROLE_OFFICE_ASSISTANT"))) {
            return true;
        }
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return false;

        // Primary: active RoomResident relationship
        if (roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(roomId, userId)) {
            return true;
        }

        if (!roomResidentRepository.existsByRoomId(roomId)) {
            return roomRepository.findById(roomId)
                    .map(r -> r.getResident() != null && r.getResident().getId().equals(userId))
                    .orElse(false);
        }

        return false;
    }

    private boolean isAuthorizedForBill(Authentication auth, Long billId) {
        if (auth == null || billId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN") || a.getAuthority().equals("ROLE_OFFICE_ASSISTANT"))) {
            return true;
        }
        return billService.getBillById(billId)
                .map(bill -> isAuthorizedForRoom(auth, bill.getRoomId()))
                .orElse(false);
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


    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    public ResponseEntity<List<Transaction>> getTransactionsByBill(@PathVariable Long billId, Authentication auth) {
        if (!isAuthorizedForBill(auth, billId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(transactionService.getTransactionsByBill(billId));
    }

    @GetMapping("/my-transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    public ResponseEntity<List<TransactionDto>> getMyTransactions(Authentication auth) {
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return ResponseEntity.status(403).build();

        List<Long> authorizedRoomIds = getAuthorizedRoomIdsForResident(userId);
        List<TransactionDto> myTransactions = authorizedRoomIds.stream()
                .flatMap(roomId -> billService.getBillsByRoom(roomId).stream())
                .flatMap(b -> transactionService.getTransactionsByBill(b.getId()).stream())
                .map(t -> {
                    MaintenanceBill bill = t.getBill();
                    String residentName = (bill != null && bill.getRoom() != null)
                            ? roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(bill.getRoom().getId())
                                    .map(rr -> rr.getResident() != null ? rr.getResident().getName() : null)
                                    .orElseGet(() -> {
                                        if (!roomResidentRepository.existsByRoomId(bill.getRoom().getId())) {
                                            return bill.getRoom().getResident() != null ? bill.getRoom().getResident().getName() : "Unknown";
                                        }
                                        return "Unknown";
                                    })
                            : "Unknown";
                    String roomNumber = bill != null && bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "Unassigned";
                    String billingMonth = bill != null ? bill.getBillingMonth() : "Unknown";
                    return new TransactionDto(
                            t.getId(),
                            bill != null ? bill.getId() : null,
                            residentName,
                            roomNumber,
                            t.getGatewayPaymentId(),
                            t.getPaymentMode(),
                            t.getAmount(),
                            t.getStatus(),
                            t.getTimestamp(),
                            billingMonth
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(myTransactions);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<List<TransactionDto>> getAllTransactions(
            @RequestParam(required = false) String financialYear,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        List<TransactionDto> list = transactionService.getAllTransactions();
        if (financialYear != null && !financialYear.isBlank()) {
            String[] parts = financialYear.trim().split("[-/]");
            if (parts.length >= 1) {
                try {
                    int y1 = Integer.parseInt(parts[0].trim());
                    int y2 = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : y1 + 1;
                    if (y2 < 100) y2 += 2000;
                    java.time.LocalDate start = java.time.LocalDate.of(y1, 4, 1);
                    java.time.LocalDate end = java.time.LocalDate.of(y2, 3, 31);
                    list = list.stream().filter(t -> {
                        if (t.getTimestamp() == null) return false;
                        java.time.LocalDate d = t.getTimestamp().toLocalDate();
                        return !d.isBefore(start) && !d.isAfter(end);
                    }).collect(Collectors.toList());
                } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/receipt-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Long id, Authentication auth) {
        Transaction tx = transactionService.getTransactionById(id)
                .orElseThrow(() -> new com.society.jaswanti.exception.ResourceNotFoundException("Transaction not found with ID: " + id));

        if (!canAccessTransaction(auth, tx)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdfBytes = pdfInvoiceGenerator.generateReceipt(tx);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt_" + (tx.getReceiptNumber() != null ? tx.getReceiptNumber() : id) + ".pdf\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/email-receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT', 'RESIDENT')")
    @AuditedAction(action = "EMAIL_RECEIPT", module = "LEDGER", summary = "Emailed a payment receipt", target = AuditTargetResolver.TRANSACTION)
    public ResponseEntity<java.util.Map<String, Object>> emailReceiptPdf(@PathVariable Long id, Authentication auth) {
        Transaction tx = transactionService.getTransactionById(id)
                .orElseThrow(() -> new com.society.jaswanti.exception.ResourceNotFoundException("Transaction not found with ID: " + id));

        if (!canAccessTransaction(auth, tx)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String targetEmail = null;
        if (tx.getBill() != null && tx.getBill().getRoom() != null) {
            targetEmail = roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(tx.getBill().getRoom().getId())
                    .map(rr -> rr.getResident() != null ? rr.getResident().getEmail() : null)
                    .orElseGet(() -> tx.getBill().getRoom().getResident() != null ? tx.getBill().getRoom().getResident().getEmail() : null);
        } else if (tx.getRecordedBy() != null) {
            targetEmail = tx.getRecordedBy().getEmail();
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "No recipient email found for transaction occupant"));
        }

        byte[] pdfBytes = pdfInvoiceGenerator.generateReceipt(tx);
        String receiptNum = tx.getReceiptNumber() != null ? tx.getReceiptNumber() : String.valueOf(tx.getId());
        emailService.sendEmailWithAttachment(
                targetEmail,
                "Official Payment Receipt: " + receiptNum,
                "Dear Resident,\n\nPlease find attached the official payment receipt for your transaction of INR " + tx.getAmount() + ".\nReceipt #: " + receiptNum,
                "receipt_" + receiptNum + ".pdf",
                pdfBytes,
                "application/pdf"
        );

        AuditTrail.describe("Emailed receipt " + receiptNum + " to " + targetEmail);
        return ResponseEntity.ok(java.util.Map.of("success", true, "sentTo", targetEmail, "receiptNumber", receiptNum));
    }

    private boolean canAccessTransaction(Authentication auth, Transaction tx) {
        if (auth == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN") || a.getAuthority().equals("ROLE_OFFICE_ASSISTANT"))) {
            return true;
        }
        Long userId = getAuthenticatedUserId(auth);
        if (userId == null) return false;

        if (tx.getRecordedBy() != null && tx.getRecordedBy().getId().equals(userId)) {
            return true;
        }

        if (tx.getBill() != null && tx.getBill().getRoom() != null) {
            return isAuthorizedForRoom(auth, tx.getBill().getRoom().getId());
        }

        return false;
    }

    @PostMapping("/expense")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    @AuditedAction(action = "RECORD_EXPENSE", module = "LEDGER", summary = "Recorded an expense", target = AuditTargetResolver.TRANSACTION)
    public ResponseEntity<TransactionDto> recordExpense(@Valid @RequestBody RecordExpenseRequest request, Authentication auth) {
        return ResponseEntity.ok(transactionService.recordExpense(request, auth));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    @AuditedAction(action = "REVERSE_TRANSACTION", module = "LEDGER", summary = "Reversed a transaction", target = AuditTargetResolver.TRANSACTION)
    public ResponseEntity<AmendmentResult> reverseTransaction(@PathVariable Long id,
                                                              @Valid @RequestBody ReverseTransactionRequest request,
                                                              Authentication auth) {
        AmendmentResult result = transactionService.reverseTransaction(id, request, auth);
        if (result.isPendingApproval()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/amendment-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<List<TransactionAmendmentRequestDto>> getAmendmentRequests() {
        return ResponseEntity.ok(transactionService.getAmendmentRequests());
    }

    @PostMapping("/amendment-requests/{id}/resolve")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @AuditedAction(action = "RESOLVE_AMENDMENT_REQUEST", module = "LEDGER", summary = "Resolved a transaction amendment request", target = AuditTargetResolver.AMENDMENT_REQUEST)
    public ResponseEntity<TransactionAmendmentRequestDto> resolveAmendmentRequest(@PathVariable Long id,
                                                                                  @Valid @RequestBody ResolveAmendmentRequest request,
                                                                                  Authentication auth) {
        return ResponseEntity.ok(transactionService.resolveAmendmentRequest(id, request, auth));
    }
}
