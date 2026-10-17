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

    public TransactionService(TransactionRepository transactionRepository, com.society.jaswanti.repository.RoomRepository roomRepository) {
        this.transactionRepository = transactionRepository;
        this.roomRepository = roomRepository;
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
            String residentName = bill != null && bill.getRoom() != null && bill.getRoom().getResident() != null ? bill.getRoom().getResident().getName() : "Unknown";
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
        }).collect(Collectors.toList());
    }
}
