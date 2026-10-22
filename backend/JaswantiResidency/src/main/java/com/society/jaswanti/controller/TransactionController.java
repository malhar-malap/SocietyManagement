package com.society.jaswanti.controller;

import com.society.jaswanti.dto.TransactionDto;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.service.MaintenanceBillService;
import com.society.jaswanti.service.TransactionService;
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
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final MaintenanceBillService billService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomResidentRepository roomResidentRepository;

    public TransactionController(TransactionService transactionService,
            MaintenanceBillService billService,
            RoomRepository roomRepository,
            UserRepository userRepository,
            RoomResidentRepository roomResidentRepository) {
        this.transactionService = transactionService;
        this.billService = billService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomResidentRepository = roomResidentRepository;
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

    private boolean isAuthorizedForBill(Authentication auth, Long billId) {
        if (auth == null || billId == null) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MASTER_ADMIN"))) {
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<Transaction> recordTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.recordTransaction(transaction));
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
    public ResponseEntity<List<Transaction>> getTransactionsByBill(@PathVariable Long billId, Authentication auth) {
        if (!isAuthorizedForBill(auth, billId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(transactionService.getTransactionsByBill(billId));
    }

    @GetMapping("/my-transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'RESIDENT')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}
