package com.society.jaswanti.service;

import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import com.society.jaswanti.dto.TransactionDto;
import com.society.jaswanti.entity.MaintenanceBill;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              com.society.jaswanti.repository.RoomRepository roomRepository,
                              com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository) {
        this.transactionRepository = transactionRepository;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
    }

    public Transaction recordTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByBill(Long billId) {
        return transactionRepository.findByBillId(billId);
    }
    
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream().map(t -> {
            MaintenanceBill bill = t.getBill();
            String residentName = resolveResidentName(bill != null ? bill.getRoom() : null);
            String roomNumber = bill != null && bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "Unassigned";
            String billingMonth = bill != null ? bill.getBillingMonth() : "Unknown";

            TransactionDto dto = new TransactionDto(
                t.getId(),
                bill != null ? bill.getId() : null,
                residentName != null ? residentName : "Unknown",
                roomNumber,
                t.getGatewayPaymentId(),
                t.getPaymentMode(),
                t.getAmount(),
                t.getStatus(),
                t.getTimestamp(),
                billingMonth
            );
            dto.setReferenceNumber(t.getReferenceNumber());
            dto.setPaymentDate(t.getPaymentDate());
            dto.setReceiptNumber(t.getReceiptNumber());
            dto.setNotes(t.getNotes());
            dto.setRecordedByName(t.getRecordedBy() != null ? t.getRecordedBy().getName() : null);
            dto.setIdempotencyKey(t.getIdempotencyKey());
            return dto;
        }).collect(Collectors.toList());
    }

    private String resolveResidentName(com.society.jaswanti.entity.Room room) {
        if (room == null) return null;
        return roomResidentRepository.findFirstByRoomIdAndIsActiveTrueOrderByIsPrimaryContactDescIdDesc(room.getId())
                .map(rr -> rr.getResident() != null ? rr.getResident().getName() : null)
                .orElseGet(() -> {
                    if (!roomResidentRepository.existsByRoomId(room.getId())) {
                        return room.getResident() != null ? room.getResident().getName() : null;
                    }
                    return null;
                });
    }
}
