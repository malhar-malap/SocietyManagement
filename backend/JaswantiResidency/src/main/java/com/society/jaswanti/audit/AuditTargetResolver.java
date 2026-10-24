package com.society.jaswanti.audit;

import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Turns a (target type, id) pair into a short human-readable label for the system log, e.g.
 * "Bill #42 · C-602 · 2026-10 · ₹3500.00 · PENDING". It runs before the action executes, so a deleted record
 * still gets a meaningful label.
 */
@Component
public class AuditTargetResolver {

    public static final String USER = "USER";
    public static final String ROOM = "ROOM";
    public static final String BILL = "BILL";
    public static final String ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String SUBMISSION = "SUBMISSION";
    public static final String PAYMENT_SUBMISSION = "PAYMENT_SUBMISSION";
    public static final String TRANSACTION = "TRANSACTION";
    public static final String OCCUPANCY = "OCCUPANCY";
    public static final String BANK_ACCOUNT = "BANK_ACCOUNT";
    public static final String AMENDMENT_REQUEST = "AMENDMENT_REQUEST";

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MaintenanceBillRepository billRepository;
    private final AnnouncementRepository announcementRepository;
    private final ComplaintRepository complaintRepository;
    private final ResidentPaymentSubmissionRepository submissionRepository;
    private final TransactionRepository transactionRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final SocietyBankAccountRepository bankAccountRepository;
    private final TransactionAmendmentRequestRepository amendmentRequestRepository;

    public AuditTargetResolver(UserRepository userRepository,
                               RoomRepository roomRepository,
                               MaintenanceBillRepository billRepository,
                               AnnouncementRepository announcementRepository,
                               ComplaintRepository complaintRepository,
                               ResidentPaymentSubmissionRepository submissionRepository,
                               TransactionRepository transactionRepository,
                               RoomResidentRepository roomResidentRepository,
                               SocietyBankAccountRepository bankAccountRepository,
                               TransactionAmendmentRequestRepository amendmentRequestRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.billRepository = billRepository;
        this.announcementRepository = announcementRepository;
        this.complaintRepository = complaintRepository;
        this.submissionRepository = submissionRepository;
        this.transactionRepository = transactionRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.amendmentRequestRepository = amendmentRequestRepository;
    }

    /** Returns a label, or null if the id is not numeric or the record does not exist. Never throws. */
    @Transactional(readOnly = true)
    public String resolve(String type, String rawId) {
        if (type == null || rawId == null) {
            return null;
        }
        Long id;
        try {
            id = Long.valueOf(rawId);
        } catch (NumberFormatException e) {
            return null;
        }
        try {
            return switch (type) {
                case USER -> userRepository.findById(id).map(AuditTargetResolver::userLabel).orElse(null);
                case ROOM -> roomRepository.findById(id).map(Room::getRoomNumber).orElse(null);
                case BILL -> billRepository.findById(id).map(b -> join("Bill #" + b.getId(), roomNumber(b.getRoom()),
                        b.getBillingMonth(), name(b.getChargeType()), money(b.getAmount()), name(b.getStatus()))).orElse(null);
                case ANNOUNCEMENT -> announcementRepository.findById(id).map(a -> "\"" + a.getTitle() + "\"").orElse(null);
                case SUBMISSION -> complaintRepository.findById(id).map(c -> join(name(c.getSubmissionType()) + " #" + c.getId(),
                        c.getCategory(), c.getResident() != null ? "by " + c.getResident().getName() : null, name(c.getStatus()))).orElse(null);
                case PAYMENT_SUBMISSION -> submissionRepository.findById(id).map(s -> join("Payment submission #" + s.getId(),
                        roomNumber(s.getRoom()), money(s.getAmount()), s.getPaymentMode(), name(s.getStatus()))).orElse(null);
                case TRANSACTION -> transactionRepository.findById(id).map(t -> join(
                        t.getReceiptNumber() != null ? "Receipt " + t.getReceiptNumber() : "Transaction #" + t.getId(),
                        t.getBill() != null ? roomNumber(t.getBill().getRoom()) : null, money(t.getAmount()), name(t.getStatus()))).orElse(null);
                case OCCUPANCY -> roomResidentRepository.findById(id).map(rr -> join("Occupancy #" + rr.getId(),
                        rr.getResident() != null ? rr.getResident().getName() : null, roomNumber(rr.getRoom()))).orElse(null);
                case BANK_ACCOUNT -> bankAccountRepository.findById(id).map(SocietyBankAccount::getAccountName).orElse(null);
                case AMENDMENT_REQUEST -> amendmentRequestRepository.findById(id).map(r -> join("Amendment request #" + r.getId(),
                        name(r.getActionType()), r.getTransaction() != null && r.getTransaction().getReceiptNumber() != null
                                ? "receipt " + r.getTransaction().getReceiptNumber() : null)).orElse(null);
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String userLabel(User u) {
        return u.getEmail() != null ? u.getName() + " <" + u.getEmail() + ">" : u.getName();
    }

    private static String roomNumber(Room room) {
        return Optional.ofNullable(room).map(Room::getRoomNumber).orElse(null);
    }

    private static String money(BigDecimal amount) {
        return amount != null ? "₹" + amount.toPlainString() : null;
    }

    private static String name(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" · ");
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }
}
