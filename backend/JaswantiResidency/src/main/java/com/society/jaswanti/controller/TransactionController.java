package com.society.jaswanti.controller;

import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final com.society.jaswanti.service.MaintenanceBillService billService;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;

    public TransactionController(TransactionService transactionService, 
            com.society.jaswanti.service.MaintenanceBillService billService,
            com.society.jaswanti.repository.RoomRepository roomRepository) {
        this.transactionService = transactionService;
        this.billService = billService;
        this.roomRepository = roomRepository;
    }

    private boolean isAuthorizedForBill(org.springframework.security.core.Authentication auth, Long billId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        com.society.jaswanti.entity.User user = (com.society.jaswanti.entity.User) auth.getPrincipal();
        return billService.getBillById(billId).map(bill -> {
            java.util.List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(user.getId());
            return rooms.stream().anyMatch(r -> r.getId().equals(bill.getRoomId()));
        }).orElse(false);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transaction> recordTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.recordTransaction(transaction));
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<Transaction>> getTransactionsByBill(@PathVariable Long billId, org.springframework.security.core.Authentication auth) {
        if (!isAuthorizedForBill(auth, billId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(transactionService.getTransactionsByBill(billId));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.society.jaswanti.dto.TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}
