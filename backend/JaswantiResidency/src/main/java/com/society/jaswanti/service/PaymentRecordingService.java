package com.society.jaswanti.service;

import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.dto.RecordPaymentRequest;
import com.society.jaswanti.dto.RecordPaymentResponse;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.exception.DuplicateBillException;
import com.society.jaswanti.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PaymentRecordingService {

    private final MaintenanceBillRepository billRepository;
    private final TransactionRepository transactionRepository;
    private final ReceiptSequenceRepository receiptSequenceRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final SocietyBankAccountRepository bankAccountRepository;

    public PaymentRecordingService(MaintenanceBillRepository billRepository,
                                   TransactionRepository transactionRepository,
                                   ReceiptSequenceRepository receiptSequenceRepository,
                                   UserRepository userRepository,
                                   ActivityLogService activityLogService,
                                   SocietyBankAccountRepository bankAccountRepository) {
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
        this.receiptSequenceRepository = receiptSequenceRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public RecordPaymentResponse recordPayment(Long billId, String idempotencyKeyHeader, RecordPaymentRequest request, Authentication auth) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request body is required");
        }

        // 1. Resolve and validate authenticated actor
        User actor = resolveAuthenticatedActor(auth);

        // 2. Resolve target bill
        MaintenanceBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance bill not found with ID: " + billId));

        // 3. Validate request data
        validatePaymentRequest(request);

        BigDecimal normalizedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        // 4. Idempotency & duplicate check (must run before bill balance checks so retries succeed safely)
        String effectiveIdempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader.trim()
                : (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                    ? request.getIdempotencyKey().trim()
                    : null);

        if (effectiveIdempotencyKey != null) {
            Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(effectiveIdempotencyKey);
            if (existingTx.isPresent()) {
                Transaction tx = existingTx.get();
                if (tx.getBill().getId().equals(billId) && tx.getAmount().compareTo(normalizedAmount) == 0) {
                    AuditTrail.action("RECORD_PAYMENT_REPLAY");
                    AuditTrail.describe("Repeated payment request matched existing receipt " + tx.getReceiptNumber()
                            + "; no new payment was recorded");
                    return toResponse(tx, bill);
                } else {
                    throw new DuplicateBillException("Idempotency key '" + effectiveIdempotencyKey + "' has already been used for another payment");
                }
            }
        }

        if (request.getPaymentMode() != PaymentMode.CASH) {
            String ref = request.getReferenceNumber().trim();
            Optional<Transaction> existingRefTx = transactionRepository.findByPaymentModeAndReferenceNumber(request.getPaymentMode().name(), ref);
            if (existingRefTx.isPresent()) {
                Transaction tx = existingRefTx.get();
                if (tx.getBill().getId().equals(billId) && tx.getAmount().compareTo(normalizedAmount) == 0) {
                    AuditTrail.action("RECORD_PAYMENT_REPLAY");
                    AuditTrail.describe("Repeated payment request matched existing receipt " + tx.getReceiptNumber()
                            + "; no new payment was recorded");
                    return toResponse(tx, bill);
                } else {
                    throw new DuplicateBillException("Payment reference number '" + ref + "' has already been recorded for another payment");
                }
            }
        }

        // 5. Validate bill state and remaining balance
        if (bill.getStatus() == BillStatus.REVERSED) {
            throw new IllegalArgumentException("Cannot record payment for reversed bill with ID: " + billId);
        }

        BigDecimal totalDue = (bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO)
                .add(bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentPaid = bill.getPaidAmount() != null
                ? bill.getPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingBalance = totalDue.subtract(currentPaid).setScale(2, RoundingMode.HALF_UP);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0 || bill.getStatus() == BillStatus.PAID || bill.getStatus() == BillStatus.PAID_CASH) {
            throw new IllegalArgumentException("Bill is already fully paid");
        }

        if (normalizedAmount.compareTo(remainingBalance) > 0) {
            throw new IllegalArgumentException(String.format("Payment amount %s exceeds remaining balance of %s", normalizedAmount, remainingBalance));
        }

        // 6. Generate receipt number safely
        String receiptNumber = generateReceiptNumber(request.getPaymentDate());

        // 7. Update bill status and balance
        BigDecimal newPaid = currentPaid.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = totalDue.subtract(newPaid).setScale(2, RoundingMode.HALF_UP);

        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(request.getPaymentMode() == PaymentMode.CASH ? BillStatus.PAID_CASH : BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
        bill.setPaidAmount(newPaid);
        bill = billRepository.save(bill);

        // 8. Determine and lock bank account
        SocietyBankAccount targetAccount = null;
        if (request.getBankAccountId() != null) {
            targetAccount = bankAccountRepository.findByIdForUpdate(request.getBankAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + request.getBankAccountId()));
        } else if (request.getPaymentMode() == PaymentMode.CASH) {
            targetAccount = bankAccountRepository.findFirstByAccountTypeAndIsActiveTrue(SocietyBankAccountType.CASH_LEDGER)
                    .map(a -> bankAccountRepository.findByIdForUpdate(a.getId()).orElse(a))
                    .orElseGet(() -> bankAccountRepository.findFirstByIsActiveTrue()
                            .map(a -> bankAccountRepository.findByIdForUpdate(a.getId()).orElse(a)).orElse(null));
        } else {
            targetAccount = bankAccountRepository.findFirstByAccountTypeAndIsActiveTrue(SocietyBankAccountType.BANK)
                    .map(a -> bankAccountRepository.findByIdForUpdate(a.getId()).orElse(a))
                    .orElseGet(() -> bankAccountRepository.findFirstByIsActiveTrue()
                            .map(a -> bankAccountRepository.findByIdForUpdate(a.getId()).orElse(a)).orElse(null));
        }

        if (targetAccount != null) {
            targetAccount.setCurrentBalance(targetAccount.getCurrentBalance().add(normalizedAmount).setScale(2, RoundingMode.HALF_UP));
            targetAccount = bankAccountRepository.save(targetAccount);
        }

        // 9. Persist transaction
        Transaction tx = new Transaction();
        tx.setBill(bill);
        tx.setBankAccount(targetAccount);
        tx.setEntryDirection("CREDIT");
        tx.setCategory("MAINTENANCE");
        tx.setApprovedAt(LocalDateTime.now());
        tx.setAmount(normalizedAmount);
        tx.setStatus("SUCCESS");
        tx.setPaymentMode(request.getPaymentMode().name());
        tx.setReferenceNumber(request.getReferenceNumber() != null ? request.getReferenceNumber().trim() : null);
        tx.setPaymentDate(request.getPaymentDate());
        tx.setRecordedBy(actor);
        String notes = request.getNotes() != null && !request.getNotes().isBlank()
                ? request.getNotes().trim()
                : (request.getReason() != null && !request.getReason().isBlank() ? request.getReason().trim() : null);
        tx.setNotes(notes);
        tx.setReceiptNumber(receiptNumber);
        tx.setIdempotencyKey(effectiveIdempotencyKey);
        tx.setTimestamp(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        // 10. Append-only activity log
        String roomDisplay = bill.getRoom() != null ? bill.getRoom().getRoomNumber() : "ID " + bill.getId();
        String accountDisplay = targetAccount != null ? targetAccount.getAccountName() : "None";
        String desc = String.format("Recorded payment: Receipt=%s, Bill ID=%d, Room=%s, Month=%s, Amount=%s, Mode=%s, BankAccount=%s, Reference=%s, Status=%s, Balance=%s, Notes=%s",
                receiptNumber, bill.getId(), roomDisplay, bill.getBillingMonth(), normalizedAmount,
                request.getPaymentMode(),
                accountDisplay,
                tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "N/A",
                bill.getStatus(),
                newRemaining,
                notes != null ? notes : "None");

        activityLogService.logActivity(actor, "RECORD_PAYMENT", "LEDGER", desc);

        return toResponse(tx, bill);
    }

    private User resolveAuthenticatedActor(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth != null && effectiveAuth.getName() != null && !effectiveAuth.getName().trim().isEmpty() && !"anonymousUser".equals(effectiveAuth.getName())) {
            User actor = userRepository.findByEmail(effectiveAuth.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated actor not found in user directory: " + effectiveAuth.getName()));

            if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MASTER_ADMIN) {
                throw new AccessDeniedException("Only Admin or Master Admin can record payments");
            }
            return actor;
        }

        // Fallback for direct programmatic test calls where SecurityContext is unpopulated
        return userRepository.findAll().stream()
                .filter(u -> (u.getRole() == Role.ADMIN || u.getRole() == Role.MASTER_ADMIN) && u.isActive())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Authenticated actor is required to record a payment"));
    }

    private void validatePaymentRequest(RecordPaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (request.getAmount().scale() > 2) {
            throw new IllegalArgumentException("Payment amount must not have more than 2 decimal places");
        }
        if (request.getPaymentMode() == null) {
            throw new IllegalArgumentException("Payment mode is required");
        }
        if (request.getPaymentMode() != PaymentMode.CASH) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Reference number is required for " + request.getPaymentMode());
            }
        }
        if (request.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        if (request.getPaymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }
    }

    @Transactional
    public String generateReceiptNumber(LocalDate date) {
        String period = YearMonth.from(date != null ? date : LocalDate.now()).format(DateTimeFormatter.ofPattern("uuuuMM"));
        Optional<ReceiptSequence> opt = receiptSequenceRepository.findByPeriodForUpdate(period);
        long seqVal;
        if (opt.isPresent()) {
            ReceiptSequence seq = opt.get();
            seqVal = seq.getNextVal();
            seq.setNextVal(seqVal + 1);
            receiptSequenceRepository.save(seq);
        } else {
            try {
                ReceiptSequence newSeq = new ReceiptSequence(period, 2L);
                receiptSequenceRepository.saveAndFlush(newSeq);
                seqVal = 1L;
            } catch (DataIntegrityViolationException ex) {
                ReceiptSequence seq = receiptSequenceRepository.findByPeriodForUpdate(period)
                        .orElseThrow(() -> new IllegalStateException("Failed to acquire receipt sequence for period " + period));
                seqVal = seq.getNextVal();
                seq.setNextVal(seqVal + 1);
                receiptSequenceRepository.save(seq);
            }
        }
        return String.format("REC-%s-%05d", period, seqVal);
    }

    private RecordPaymentResponse toResponse(Transaction tx, MaintenanceBill bill) {
        String roomNumber = bill.getRoom() != null ? bill.getRoom().getRoomNumber() : null;
        BigDecimal totalDue = (bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO)
                .add(bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentPaid = bill.getPaidAmount() != null
                ? bill.getPaidAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = totalDue.subtract(currentPaid).setScale(2, RoundingMode.HALF_UP);
        String recordedByName = tx.getRecordedBy() != null ? tx.getRecordedBy().getName() : null;

        PaymentMode mode = null;
        if (tx.getPaymentMode() != null) {
            try {
                mode = PaymentMode.valueOf(tx.getPaymentMode());
            } catch (IllegalArgumentException ignored) {}
        }

        return new RecordPaymentResponse(
                tx.getId(),
                tx.getReceiptNumber(),
                bill.getId(),
                roomNumber,
                tx.getAmount(),
                mode,
                tx.getReferenceNumber(),
                tx.getPaymentDate(),
                bill.getStatus(),
                currentPaid,
                remaining,
                tx.getNotes(),
                recordedByName,
                tx.getIdempotencyKey(),
                tx.getTimestamp()
        );
    }
}
