package com.society.jaswanti.service;

import com.society.jaswanti.dto.*;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final SocietyBankAccountRepository bankAccountRepository;
    private final TransactionAmendmentRequestRepository amendmentRequestRepository;
    private final MaintenanceBillRepository billRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public TransactionService(TransactionRepository transactionRepository,
                              RoomRepository roomRepository,
                              RoomResidentRepository roomResidentRepository,
                              SocietyBankAccountRepository bankAccountRepository,
                              TransactionAmendmentRequestRepository amendmentRequestRepository,
                              MaintenanceBillRepository billRepository,
                              UserRepository userRepository,
                              ActivityLogService activityLogService) {
        this.transactionRepository = transactionRepository;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.amendmentRequestRepository = amendmentRequestRepository;
        this.billRepository = billRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }


    public List<Transaction> getTransactionsByBill(Long billId) {
        return transactionRepository.findByBillId(billId);
    }

    public java.util.Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
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

            dto.setEntryDirection(t.getEntryDirection() != null ? t.getEntryDirection() : "CREDIT");
            dto.setCategory(t.getCategory() != null ? t.getCategory() : "MAINTENANCE");
            dto.setApprovedAt(t.getApprovedAt() != null ? t.getApprovedAt() : t.getTimestamp());

            LocalDateTime appAt = t.getApprovedAt() != null ? t.getApprovedAt() : t.getTimestamp();
            boolean locked = appAt != null && Duration.between(appAt, LocalDateTime.now()).toHours() >= 24;
            dto.setLocked(locked);

            if (t.getBankAccount() != null) {
                dto.setBankAccountId(t.getBankAccount().getId());
                dto.setBankAccountName(t.getBankAccount().getAccountName());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionDto recordExpense(RecordExpenseRequest request, Authentication auth) {
        User actor = resolveAuthenticatedActor(auth);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
        if (request.getAmount().scale() > 2) {
            throw new IllegalArgumentException("Expense amount cannot have more than 2 decimal places");
        }
        if (request.getPaymentDate() == null || request.getPaymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expense payment date cannot be null or in the future");
        }
        if (request.getPaymentMode() != PaymentMode.CASH) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Reference number is required for " + request.getPaymentMode());
            }
        }

        BigDecimal normalizedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        SocietyBankAccount account = bankAccountRepository.findByIdForUpdate(request.getBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + request.getBankAccountId()));

        if (!account.isActive()) {
            throw new IllegalArgumentException("Selected bank account is inactive");
        }

        // DEBIT: subtract amount from current balance
        account.setCurrentBalance(account.getCurrentBalance().subtract(normalizedAmount).setScale(2, RoundingMode.HALF_UP));
        account = bankAccountRepository.save(account);

        String voucherNumber = String.format("EXP-%s-%04d",
                LocalDate.now().format(DateTimeFormatter.ofPattern("uuuuMM")),
                (System.currentTimeMillis() % 10000));

        Transaction tx = new Transaction();
        tx.setBill(null);
        tx.setBankAccount(account);
        tx.setAmount(normalizedAmount);
        tx.setEntryDirection("DEBIT");
        tx.setCategory(request.getCategory() != null ? request.getCategory().name() : TransactionCategory.UTILITY_EXPENSE.name());
        tx.setStatus("SUCCESS");
        tx.setPaymentMode(request.getPaymentMode().name());
        tx.setReferenceNumber(request.getReferenceNumber() != null ? request.getReferenceNumber().trim() : null);
        tx.setPaymentDate(request.getPaymentDate());
        tx.setRecordedBy(actor);

        String combinedNotes = (request.getPayeeName() != null && !request.getPayeeName().isBlank()
                ? "Payee: " + request.getPayeeName().trim() + ". " : "")
                + (request.getNotes() != null ? request.getNotes().trim() : "");
        tx.setNotes(combinedNotes.trim());
        tx.setReceiptNumber(voucherNumber);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        tx.setApprovedAt(LocalDateTime.now());
        tx.setTimestamp(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        activityLogService.logActivity(actor, "RECORD_EXPENSE", "LEDGER",
                String.format("Recorded expense: Voucher=%s, Category=%s, Amount=₹%s, BankAccount=%s, Payee=%s, Ref=%s, NewBalance=₹%s",
                        voucherNumber, tx.getCategory(), normalizedAmount.toPlainString(), account.getAccountName(),
                        request.getPayeeName() != null ? request.getPayeeName() : "N/A",
                        tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "N/A",
                        account.getCurrentBalance().toPlainString()));

        TransactionDto dto = new TransactionDto(
                tx.getId(), null, null, null, null, tx.getPaymentMode(),
                tx.getAmount(), tx.getStatus(), tx.getTimestamp(), null
        );
        dto.setReferenceNumber(tx.getReferenceNumber());
        dto.setPaymentDate(tx.getPaymentDate());
        dto.setReceiptNumber(tx.getReceiptNumber());
        dto.setNotes(tx.getNotes());
        dto.setRecordedByName(actor.getName());
        dto.setEntryDirection(tx.getEntryDirection());
        dto.setCategory(tx.getCategory());
        dto.setApprovedAt(tx.getApprovedAt());
        dto.setBankAccountId(account.getId());
        dto.setBankAccountName(account.getAccountName());
        dto.setLocked(false);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public AmendmentResult reverseTransaction(Long transactionId, ReverseTransactionRequest request, Authentication auth) {
        User actor = resolveAuthenticatedActor(auth);

        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reversal reason is required");
        }

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

        if ("REVERSED".equalsIgnoreCase(tx.getStatus()) || "REVERSAL".equalsIgnoreCase(tx.getStatus())) {
            throw new IllegalStateException("Transaction has already been reversed");
        }

        boolean isMasterAdmin = (actor.getRole() == Role.MASTER_ADMIN);
        LocalDateTime approvedAt = tx.getApprovedAt() != null ? tx.getApprovedAt() : tx.getTimestamp();
        boolean isLocked = (approvedAt != null && Duration.between(approvedAt, LocalDateTime.now()).toHours() >= 24);

        if (!isMasterAdmin && isLocked) {
            // Queue for Master Admin approval (24-Hour Admin Edit Lock)
            TransactionAmendmentRequest req = new TransactionAmendmentRequest(
                    tx, actor, "REVERSE", null, request.getReason().trim()
            );
            req = amendmentRequestRepository.save(req);

            activityLogService.logActivity(actor, "QUEUE_REVERSAL_AMENDMENT", "LEDGER",
                    String.format("Queued reversal amendment request for transaction ID %d (approved >24h ago). Reason: %s",
                            transactionId, request.getReason().trim()));

            return new AmendmentResult(true, "Transaction approved more than 24 hours ago. Reversal amendment has been queued for Master Admin approval.", req.getId());
        }

        // Within 24 hours OR Master Admin: execute immediately
        applyReversal(tx, actor, request.getReason().trim());
        return new AmendmentResult(false, "Transaction reversed successfully", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyReversal(Transaction tx, User actor, String reason) {
        // 1. Recalculate SocietyBankAccount current_balance
        if (tx.getBankAccount() != null) {
            SocietyBankAccount account = bankAccountRepository.findByIdForUpdate(tx.getBankAccount().getId())
                    .orElse(tx.getBankAccount());

            if ("CREDIT".equalsIgnoreCase(tx.getEntryDirection())) {
                // Reversing income/credit: subtract from balance
                account.setCurrentBalance(account.getCurrentBalance().subtract(tx.getAmount()).setScale(2, RoundingMode.HALF_UP));
            } else {
                // Reversing expense/debit: add back to balance
                account.setCurrentBalance(account.getCurrentBalance().add(tx.getAmount()).setScale(2, RoundingMode.HALF_UP));
            }
            bankAccountRepository.save(account);
        }

        // 2. If tied to a MaintenanceBill, restore bill balance and status
        if (tx.getBill() != null) {
            MaintenanceBill bill = tx.getBill();
            if ("CREDIT".equalsIgnoreCase(tx.getEntryDirection())) {
                BigDecimal currentPaid = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal newPaid = currentPaid.subtract(tx.getAmount()).setScale(2, RoundingMode.HALF_UP);
                if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                    newPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
                bill.setPaidAmount(newPaid);

                if (newPaid.compareTo(BigDecimal.ZERO) == 0) {
                    bill.setStatus(BillStatus.PENDING);
                } else {
                    bill.setStatus(BillStatus.PARTIALLY_PAID);
                }
                billRepository.save(bill);
            }
        }

        // 3. Mark original transaction as REVERSED
        tx.setStatus("REVERSED");
        transactionRepository.save(tx);

        // 4. Create compensating reversal transaction (never overwrite history)
        Transaction revTx = new Transaction();
        revTx.setBill(tx.getBill());
        revTx.setBankAccount(tx.getBankAccount());
        revTx.setAmount(tx.getAmount());
        revTx.setEntryDirection("CREDIT".equalsIgnoreCase(tx.getEntryDirection()) ? "DEBIT" : "CREDIT");
        revTx.setCategory(tx.getCategory());
        revTx.setStatus("REVERSAL");
        revTx.setPaymentMode(tx.getPaymentMode());
        revTx.setReferenceNumber("REV-" + (tx.getReferenceNumber() != null ? tx.getReferenceNumber() : tx.getId()));
        revTx.setPaymentDate(LocalDate.now());
        revTx.setRecordedBy(actor);
        revTx.setNotes("Reversal of TX ID " + tx.getId() + ". Reason: " + reason);
        revTx.setApprovedAt(LocalDateTime.now());
        revTx.setTimestamp(LocalDateTime.now());
        transactionRepository.save(revTx);

        // 5. Audit log
        activityLogService.logActivity(actor, "REVERSE_TRANSACTION", "LEDGER",
                String.format("Reversed transaction ID %d (amount: ₹%s, direction: %s). Reason: %s",
                        tx.getId(), tx.getAmount().toPlainString(), tx.getEntryDirection(), reason));
    }

    public List<TransactionAmendmentRequestDto> getAmendmentRequests() {
        return amendmentRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> new TransactionAmendmentRequestDto(
                        r.getId(),
                        r.getTransaction() != null ? r.getTransaction().getId() : null,
                        r.getRequestedByAdmin() != null ? r.getRequestedByAdmin().getName() : null,
                        r.getActionType(),
                        r.getProposedPayloadJson(),
                        r.getReason(),
                        r.getStatus(),
                        r.getReviewedByMasterAdmin() != null ? r.getReviewedByMasterAdmin().getName() : null,
                        r.getCreatedAt(),
                        r.getResolvedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionAmendmentRequestDto resolveAmendmentRequest(Long requestId, ResolveAmendmentRequest request, Authentication auth) {
        User masterAdmin = resolveMasterAdmin(auth);

        TransactionAmendmentRequest req = amendmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Amendment request not found with ID: " + requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("Amendment request has already been resolved with status: " + req.getStatus());
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            if ("REVERSE".equalsIgnoreCase(req.getActionType())) {
                applyReversal(req.getTransaction(), masterAdmin, "Approved Master Admin amendment: " + req.getReason());
            }
            req.setStatus("APPROVED");
        } else {
            req.setStatus("REJECTED");
        }

        req.setReviewedByMasterAdmin(masterAdmin);
        req.setResolvedAt(LocalDateTime.now());
        req = amendmentRequestRepository.save(req);

        activityLogService.logActivity(masterAdmin, "RESOLVE_AMENDMENT_REQUEST", "LEDGER",
                String.format("Resolved amendment request ID %d for transaction ID %d with decision %s. Notes: %s",
                        requestId, req.getTransaction().getId(), req.getStatus(),
                        request.getReviewNotes() != null ? request.getReviewNotes() : "None"));

        return new TransactionAmendmentRequestDto(
                req.getId(),
                req.getTransaction().getId(),
                req.getRequestedByAdmin() != null ? req.getRequestedByAdmin().getName() : null,
                req.getActionType(),
                req.getProposedPayloadJson(),
                req.getReason(),
                req.getStatus(),
                masterAdmin.getName(),
                req.getCreatedAt(),
                req.getResolvedAt()
        );
    }

    private User resolveAuthenticatedActor(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth != null && effectiveAuth.getName() != null && !effectiveAuth.getName().trim().isEmpty() && !"anonymousUser".equals(effectiveAuth.getName())) {
            User user = userRepository.findByEmail(effectiveAuth.getName())
                    .orElseThrow(() -> new AccessDeniedException("User not found: " + effectiveAuth.getName()));
            if (!user.getRole().isStaff()) {
                throw new AccessDeniedException("Only Admin or Master Admin can perform ledger actions");
            }
            return user;
        }

        return userRepository.findAll().stream()
                .filter(u -> u.getRole().isStaff() && u.isActive())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Authenticated Admin actor is required"));
    }

    private User resolveMasterAdmin(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth != null && effectiveAuth.getName() != null && !effectiveAuth.getName().trim().isEmpty() && !"anonymousUser".equals(effectiveAuth.getName())) {
            User user = userRepository.findByEmail(effectiveAuth.getName())
                    .orElseThrow(() -> new AccessDeniedException("User not found: " + effectiveAuth.getName()));
            if (user.getRole() != Role.MASTER_ADMIN) {
                throw new AccessDeniedException("Only Master Admin can resolve ledger amendment requests");
            }
            return user;
        }

        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MASTER_ADMIN && u.isActive())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Master Admin authentication is required"));
    }

    private String resolveResidentName(Room room) {
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
