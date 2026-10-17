package com.society.jaswanti.service;

import com.society.jaswanti.dto.MaintenanceBillDto;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
public class MaintenanceBillService {
    private final MaintenanceBillRepository billRepository;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final com.society.jaswanti.repository.TransactionRepository transactionRepository;

    public MaintenanceBillService(MaintenanceBillRepository billRepository, 
                                  com.society.jaswanti.repository.RoomRepository roomRepository,
                                  com.society.jaswanti.repository.TransactionRepository transactionRepository) {
        this.billRepository = billRepository;
        this.roomRepository = roomRepository;
        this.transactionRepository = transactionRepository;
    }

    public MaintenanceBillDto createBill(MaintenanceBill bill) {
        return convertToDto(billRepository.save(bill));
    }

    public List<MaintenanceBillDto> generateMonthlyBills(String billingMonth, Double amount, java.time.LocalDate dueDate) {
        List<com.society.jaswanti.entity.Room> rooms = roomRepository.findAll();
        List<MaintenanceBill> newBills = new java.util.ArrayList<>();
        
        for (com.society.jaswanti.entity.Room room : rooms) {
            // Check for duplicate bill
            boolean exists = billRepository.findByRoomId(room.getId()).stream()
                    .anyMatch(b -> b.getBillingMonth().equals(billingMonth));
            if (!exists) {
                MaintenanceBill bill = new MaintenanceBill();
                bill.setRoom(room);
                bill.setBillingMonth(billingMonth);
                bill.setAmount(amount);
                bill.setDueDate(dueDate);
                bill.setStatus(com.society.jaswanti.entity.BillStatus.PENDING);
                newBills.add(bill);
            }
        }
        return billRepository.saveAll(newBills).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public java.util.Optional<MaintenanceBillDto> updateBill(Long id, MaintenanceBill updatedBill) {
        return billRepository.findById(id).map(bill -> {
            if (updatedBill.getAmount() != null) bill.setAmount(updatedBill.getAmount());
            if (updatedBill.getDueDate() != null) bill.setDueDate(updatedBill.getDueDate());
            if (updatedBill.getLateFee() != null) bill.setLateFee(updatedBill.getLateFee());
            return convertToDto(billRepository.save(bill));
        });
    }

    public java.util.Optional<MaintenanceBillDto> markPaidCash(Long id) {
        return billRepository.findById(id).map(bill -> {
            if (bill.getStatus() == com.society.jaswanti.entity.BillStatus.PAID) {
                return convertToDto(bill); // Already paid
            }
            bill.setStatus(com.society.jaswanti.entity.BillStatus.PAID);
            
            com.society.jaswanti.entity.Transaction t = new com.society.jaswanti.entity.Transaction(
                bill, 
                bill.getAmount() + (bill.getLateFee() != null ? bill.getLateFee() : 0.0), 
                "SUCCESS", 
                "CASH"
            );
            t.setGatewayPaymentId("CASH_" + java.util.UUID.randomUUID().toString().substring(0, 8));
            transactionRepository.save(t);
            
            return convertToDto(billRepository.save(bill));
        });
    }

    public java.util.Optional<MaintenanceBillDto> getBillById(Long id) {
        return billRepository.findById(id).map(this::convertToDto);
    }

    public java.util.Optional<MaintenanceBill> getBillEntityById(Long id) {
        return billRepository.findById(id);
    }

    public List<MaintenanceBillDto> getBillsByRoom(Long roomId) {
        return billRepository.findByRoomId(roomId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<MaintenanceBillDto> getAllBills() {
        return billRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Page<MaintenanceBillDto> getBillsPaginated(Specification<MaintenanceBill> spec, Pageable pageable) {
        return billRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    private MaintenanceBillDto convertToDto(MaintenanceBill bill) {
        String roomNumber = bill.getRoom() != null ? bill.getRoom().getRoomNumber() : null;
        String residentName = (bill.getRoom() != null && bill.getRoom().getResident() != null) 
                ? bill.getRoom().getResident().getName() : null;
        java.time.LocalDate issueDate = bill.getDueDate() != null ? bill.getDueDate().minusDays(15) : null;

        return new MaintenanceBillDto(
                bill.getId(),
                bill.getRoom() != null ? bill.getRoom().getId() : null,
                roomNumber,
                residentName,
                bill.getBillingMonth(),
                bill.getAmount(),
                bill.getDueDate(),
                issueDate,
                bill.getLateFee(),
                bill.getStatus()
        );
    }
}
