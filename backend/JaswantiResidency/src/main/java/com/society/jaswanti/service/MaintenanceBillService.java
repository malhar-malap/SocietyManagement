package com.society.jaswanti.service;

import com.society.jaswanti.dto.MaintenanceBillDto;
import com.society.jaswanti.entity.ChargeType;
import com.society.jaswanti.entity.MaintenanceBill;
import com.society.jaswanti.exception.DuplicateBillException;
import com.society.jaswanti.repository.MaintenanceBillRepository;
import com.society.jaswanti.util.BillingPeriodHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MaintenanceBillService {
    private final MaintenanceBillRepository billRepository;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final com.society.jaswanti.repository.TransactionRepository transactionRepository;
    private final com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;
    private final PaymentRecordingService paymentRecordingService;

    public MaintenanceBillService(MaintenanceBillRepository billRepository,
                                  com.society.jaswanti.repository.RoomRepository roomRepository,
                                  com.society.jaswanti.repository.TransactionRepository transactionRepository,
                                  com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository,
                                  PaymentRecordingService paymentRecordingService) {
        this.billRepository = billRepository;
        this.roomRepository = roomRepository;
        this.transactionRepository = transactionRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.paymentRecordingService = paymentRecordingService;
    }

    @Transactional
    public MaintenanceBillDto createBill(MaintenanceBill bill) {
        if (bill.getRoom() == null || bill.getRoom().getId() == null) {
            throw new IllegalArgumentException("Room must be provided for maintenance bill");
        }
        YearMonth ym = BillingPeriodHelper.validateCanonical(bill.getBillingMonth());
        String canonicalMonth = ym.toString();
        bill.setBillingMonth(canonicalMonth);

        ChargeType chargeType = bill.getChargeType() != null ? bill.getChargeType() : ChargeType.MAINTENANCE;
        bill.setChargeType(chargeType);

        Set<String> equivalents = BillingPeriodHelper.getEquivalentRepresentations(ym);
        if (billRepository.existsByRoomIdAndBillingMonthInAndChargeType(bill.getRoom().getId(), equivalents, chargeType)) {
            String roomDisplay = bill.getRoom().getRoomNumber() != null ? bill.getRoom().getRoomNumber() : ("ID " + bill.getRoom().getId());
            throw new DuplicateBillException("A bill of type " + chargeType + " already exists for room " + roomDisplay + " and month " + canonicalMonth);
        }

        try {
            return convertToDto(billRepository.save(bill));
        } catch (DataIntegrityViolationException ex) {
            String roomDisplay = bill.getRoom().getRoomNumber() != null ? bill.getRoom().getRoomNumber() : ("ID " + bill.getRoom().getId());
            throw new DuplicateBillException("A bill of type " + chargeType + " already exists for room " + roomDisplay + " and month " + canonicalMonth);
        }
    }

    @Transactional
    public List<MaintenanceBillDto> generateMonthlyBills(String billingMonth, BigDecimal amount, LocalDate dueDate, ChargeType chargeType) {
        YearMonth ym = BillingPeriodHelper.validateCanonical(billingMonth);
        String canonicalMonth = ym.toString();
        Set<String> equivalents = BillingPeriodHelper.getEquivalentRepresentations(ym);

        ChargeType type = chargeType != null ? chargeType : ChargeType.MAINTENANCE;
        BigDecimal normalizedAmount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
        List<com.society.jaswanti.entity.Room> rooms = roomRepository.findAll();
        List<MaintenanceBill> newBills = new java.util.ArrayList<>();

        for (com.society.jaswanti.entity.Room room : rooms) {
            boolean exists = billRepository.existsByRoomIdAndBillingMonthInAndChargeType(room.getId(), equivalents, type);
            if (!exists) {
                MaintenanceBill bill = new MaintenanceBill();
                bill.setRoom(room);
                bill.setBillingMonth(canonicalMonth);
                bill.setAmount(normalizedAmount);
                bill.setDueDate(dueDate);
                bill.setStatus(com.society.jaswanti.entity.BillStatus.PENDING);
                bill.setChargeType(type);
                newBills.add(bill);
            }

            // Phase 2B: Automatic companion NOC bill generation for commercial/applicable units
            if (type == ChargeType.MAINTENANCE && room.isNocApplicable() && room.getNocAmount() != null && room.getNocAmount().compareTo(BigDecimal.ZERO) > 0) {
                boolean nocExists = billRepository.existsByRoomIdAndBillingMonthInAndChargeType(room.getId(), equivalents, ChargeType.NOC);
                if (!nocExists) {
                    MaintenanceBill nocBill = new MaintenanceBill();
                    nocBill.setRoom(room);
                    nocBill.setBillingMonth(canonicalMonth);
                    nocBill.setAmount(room.getNocAmount().setScale(2, RoundingMode.HALF_UP));
                    nocBill.setDueDate(dueDate);
                    nocBill.setStatus(com.society.jaswanti.entity.BillStatus.PENDING);
                    nocBill.setChargeType(ChargeType.NOC);
                    newBills.add(nocBill);
                }
            }
        }
        try {
            return billRepository.saveAll(newBills).stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBillException("Duplicate bill detected during batch generation for month " + canonicalMonth + " and charge type " + type);
        }
    }

    @Transactional
    public List<MaintenanceBillDto> generateMonthlyBills(String billingMonth, BigDecimal amount, LocalDate dueDate) {
        return generateMonthlyBills(billingMonth, amount, dueDate, ChargeType.MAINTENANCE);
    }

    @Transactional
    public List<MaintenanceBillDto> generateMonthlyBills(String billingMonth, Double amount, LocalDate dueDate) {
        return generateMonthlyBills(billingMonth, amount != null ? BigDecimal.valueOf(amount) : null, dueDate, ChargeType.MAINTENANCE);
    }

    @Transactional
    public List<MaintenanceBillDto> generateMonthlyBills(String billingMonth, Double amount, LocalDate dueDate, ChargeType chargeType) {
        return generateMonthlyBills(billingMonth, amount != null ? BigDecimal.valueOf(amount) : null, dueDate, chargeType);
    }

    public java.util.Optional<MaintenanceBillDto> updateBill(Long id, MaintenanceBill updatedBill) {
        return billRepository.findById(id).map(bill -> {
            if (updatedBill.getBillingMonth() != null) {
                YearMonth ym = BillingPeriodHelper.validateCanonical(updatedBill.getBillingMonth());
                bill.setBillingMonth(ym.toString());
            }
            if (updatedBill.getAmount() != null) bill.setAmount(updatedBill.getAmount());
            if (updatedBill.getDueDate() != null) bill.setDueDate(updatedBill.getDueDate());
            if (updatedBill.getLateFee() != null) bill.setLateFee(updatedBill.getLateFee());
            return convertToDto(billRepository.save(bill));
        });
    }

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public java.util.Optional<MaintenanceBillDto> markPaidCash(Long id) {
        return markPaidCash(id, null);
    }

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public java.util.Optional<MaintenanceBillDto> markPaidCash(Long id, org.springframework.security.core.Authentication auth) {
        java.util.Optional<MaintenanceBill> billOpt = billRepository.findById(id);
        if (billOpt.isEmpty()) {
            return java.util.Optional.empty();
        }
        MaintenanceBill bill = billOpt.get();
        if (bill.getStatus() == com.society.jaswanti.entity.BillStatus.PAID || bill.getStatus() == com.society.jaswanti.entity.BillStatus.PAID_CASH) {
            return java.util.Optional.of(convertToDto(bill)); // Already fully paid, return idempotent state
        }

        BigDecimal remaining = bill.getBalance();
        com.society.jaswanti.dto.RecordPaymentRequest request = new com.society.jaswanti.dto.RecordPaymentRequest(
                remaining,
                com.society.jaswanti.entity.PaymentMode.CASH,
                null,
                java.time.LocalDate.now(),
                "Legacy cash payment via mark-paid-cash"
        );
        String idempotencyKey = "LEGACY-CASH-" + id;
        paymentRecordingService.recordPayment(id, idempotencyKey, request, auth);

        return billRepository.findById(id).map(this::convertToDto);
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

    public List<MaintenanceBill> getAllBillEntities() {
        return billRepository.findAll();
    }

    public Page<MaintenanceBillDto> getBillsPaginated(Specification<MaintenanceBill> spec, Pageable pageable) {
        return billRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    private MaintenanceBillDto convertToDto(MaintenanceBill bill) {
        String roomNumber = bill.getRoom() != null ? bill.getRoom().getRoomNumber() : null;
        String residentName = resolveResidentName(bill.getRoom());
        LocalDate issueDate = bill.getDueDate() != null ? bill.getDueDate().minusDays(15) : null;

        MaintenanceBillDto dto = new MaintenanceBillDto(
                bill.getId(),
                bill.getRoom() != null ? bill.getRoom().getId() : null,
                roomNumber,
                residentName,
                bill.getBillingMonth(),
                bill.getAmount(),
                bill.getDueDate(),
                issueDate,
                bill.getLateFee(),
                bill.getStatus(),
                bill.getChargeType()
        );
        dto.setPaidAmount(bill.getPaidAmount());
        dto.setBalance(bill.getBalance());
        return dto;
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
