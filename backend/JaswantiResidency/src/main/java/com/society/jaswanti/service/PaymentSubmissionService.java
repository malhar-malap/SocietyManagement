package com.society.jaswanti.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.society.jaswanti.dto.*;
import com.society.jaswanti.entity.*;
import com.society.jaswanti.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSubmissionService.class);

    private final ResidentPaymentSubmissionRepository submissionRepository;
    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;
    private final MaintenanceBillRepository billRepository;
    private final SocietyBankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final PaymentRecordingService paymentRecordingService;
    private final ActivityLogService activityLogService;
    private final FileValidationService fileValidationService;
    private final SystemNotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentSubmissionService(ResidentPaymentSubmissionRepository submissionRepository,
                                    RoomRepository roomRepository,
                                    RoomResidentRepository roomResidentRepository,
                                    MaintenanceBillRepository billRepository,
                                    SocietyBankAccountRepository bankAccountRepository,
                                    UserRepository userRepository,
                                    PaymentRecordingService paymentRecordingService,
                                    ActivityLogService activityLogService,
                                    FileValidationService fileValidationService,
                                    SystemNotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.billRepository = billRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.paymentRecordingService = paymentRecordingService;
        this.activityLogService = activityLogService;
        this.fileValidationService = fileValidationService;
        this.notificationService = notificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResidentPaymentSubmissionDto createSubmission(CreatePaymentSubmissionRequest request,
                                                         MultipartFile screenshot,
                                                         Authentication auth) throws IOException {
        User resident = resolveAuthenticatedUser(auth);

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + request.getRoomId()));

        // Enforce active resident relationship
        boolean isAuthorized = roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(room.getId(), resident.getId());
        if (!isAuthorized && !roomResidentRepository.existsByRoomId(room.getId())) {
            isAuthorized = (room.getResident() != null && room.getResident().getId().equals(resident.getId()));
        }
        if (!isAuthorized && !resident.getRole().isStaff()) {
            throw new AccessDeniedException("You are not an authorized active resident of room " + room.getRoomNumber());
        }

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        BigDecimal normalizedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        PaymentMode paymentMode = parsePaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "UPI_MANUAL");
        List<Long> selectedBillIds = request.getSelectedBillIds() != null
                ? request.getSelectedBillIds().stream().distinct().toList() : List.of();
        validateSelectedBills(selectedBillIds, room, true);

        ResidentPaymentSubmission submission = new ResidentPaymentSubmission();
        submission.setRoom(room);
        submission.setSubmittedByUser(resident);
        submission.setAmount(normalizedAmount);
        submission.setPaymentMode(paymentMode.name());
        submission.setReferenceNumber(request.getReferenceNumber() != null ? request.getReferenceNumber().trim() : null);
        submission.setResidentDescription(request.getResidentDescription() != null ? request.getResidentDescription().trim() : null);
        submission.setAdvanceMonthsCount(Math.max(0, request.getAdvanceMonthsCount()));
        submission.setStatus("PENDING");
        submission.setCreatedAt(LocalDateTime.now());

        if (!selectedBillIds.isEmpty()) {
            submission.setSelectedBillIdsJson(objectMapper.writeValueAsString(selectedBillIds));
        }

        if (screenshot != null && !screenshot.isEmpty()) {
            String detectedType = fileValidationService.validateImageOrPdf(screenshot);
            submission.setScreenshotData(screenshot.getBytes());
            submission.setScreenshotFilename(screenshot.getOriginalFilename());
            submission.setScreenshotContentType(detectedType);
        }

        submission = submissionRepository.save(submission);

        activityLogService.logActivity(resident, "SUBMIT_PAYMENT", "RESIDENT_PAYMENT",
                String.format("Resident %s submitted payment of ₹%s for Room %s (Mode: %s, Ref: %s)",
                        resident.getName(), normalizedAmount.toPlainString(), room.getRoomNumber(),
                        submission.getPaymentMode(), submission.getReferenceNumber()));

        try {
            List<User> activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole().isStaff()
                            && u.isActive()
                            && u.getStatus() == UserStatus.ACTIVE)
                    .toList();
            String title = "New payment proof";
            String message = String.format("%s submitted ₹%s (%s) for review.",
                    room.getRoomNumber(), normalizedAmount.toPlainString(), submission.getPaymentMode());
            for (User adminUser : activeAdmins) {
                notificationService.createNotification(adminUser, title, message, "/admin/billing");
            }
        } catch (RuntimeException e) {
            logger.warn("Could not create notification for payment submission: {}", e.getMessage());
        }

        return toDto(submission);
    }

    public List<ResidentPaymentSubmissionDto> getMySubmissions(Authentication auth) {
        User resident = resolveAuthenticatedUser(auth);
        return submissionRepository.findBySubmittedByUserIdOrderByCreatedAtDesc(resident.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ResidentPaymentSubmissionDto> getAllSubmissions() {
        return submissionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ResidentPaymentSubmission getSubmissionEntity(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new com.society.jaswanti.exception.ResourceNotFoundException("Submission not found with ID: " + id));
    }

    public ResidentPaymentSubmission getAuthorizedSubmissionForScreenshot(Long id, Authentication auth) {
        ResidentPaymentSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new com.society.jaswanti.exception.ResourceNotFoundException("Submission not found with ID: " + id));

        User actor = resolveAuthenticatedUser(auth);
        if (actor.getRole().isStaff()) {
            return submission;
        }

        if (submission.getSubmittedByUser() != null && submission.getSubmittedByUser().getId().equals(actor.getId())) {
            return submission;
        }

        Room room = submission.getRoom();
        if (room != null) {
            boolean isRoomResident = roomResidentRepository.existsByRoomIdAndResidentIdAndIsActiveTrue(room.getId(), actor.getId());
            if (!isRoomResident && !roomResidentRepository.existsByRoomId(room.getId())) {
                isRoomResident = (room.getResident() != null && room.getResident().getId().equals(actor.getId()));
            }
            if (isRoomResident) {
                return submission;
            }
        }

        throw new AccessDeniedException("You are not authorized to access this payment screenshot");
    }

    @Transactional(rollbackFor = Exception.class)
    public ResidentPaymentSubmissionDto approveSubmission(Long id, ApprovePaymentSubmissionRequest request, Authentication auth) {
        User admin = resolveAdminActor(auth);

        if (request.getReceivedBankAccountId() == null) {
            throw new IllegalArgumentException("Received Bank Account ID is mandatory for approval");
        }

        ResidentPaymentSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment submission not found with ID: " + id));

        if (!"PENDING".equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException("Submission has already been " + submission.getStatus());
        }

        SocietyBankAccount bankAccount = bankAccountRepository.findByIdForUpdate(request.getReceivedBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + request.getReceivedBankAccountId()));

        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Selected bank account is inactive");
        }

        // An unknown mode used to fall back to CASH silently, recording the money in the cash ledger
        PaymentMode mode = parsePaymentMode(submission.getPaymentMode());

        List<Long> billIds = new ArrayList<>();
        if (submission.getSelectedBillIdsJson() != null && !submission.getSelectedBillIdsJson().isBlank()) {
            try {
                billIds = objectMapper.readValue(submission.getSelectedBillIdsJson(), new TypeReference<List<Long>>() {});
            } catch (Exception e) {
                // Falling back to "settle any pending bills" would apply the money somewhere the resident didn't choose
                throw new IllegalArgumentException("Submission #" + submission.getId() + " has an unreadable bill selection; reject it and ask the resident to resubmit");
            }
        }

        List<MaintenanceBill> billsToSettle = new ArrayList<>();
        if (!billIds.isEmpty()) {
            // Re-checked at approval: the selection must still belong to this room (bills may have been paid since)
            billsToSettle = validateSelectedBills(billIds, submission.getRoom(), false);
        } else {
            // Settle existing pending bills for this room
            billsToSettle = billRepository.findByRoomId(submission.getRoom().getId()).stream()
                    .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.PARTIALLY_PAID)
                    .sorted(Comparator.comparing(MaintenanceBill::getBillingMonth))
                    .collect(Collectors.toList());
        }

        BigDecimal remainingToAllocate = submission.getAmount();
        List<Long> txIds = new ArrayList<>();
        String lastReceiptNumber = null;
        String baseRef = (submission.getReferenceNumber() != null && !submission.getReferenceNumber().isBlank())
                ? submission.getReferenceNumber().trim()
                : "SUB-" + submission.getId();

        // 1. Settle selected / outstanding bills
        for (MaintenanceBill bill : billsToSettle) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal billDue = (bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO)
                    .add(bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO)
                    .subtract(bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);

            if (billDue.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal payAmount = remainingToAllocate.min(billDue);

            RecordPaymentRequest payReq = new RecordPaymentRequest();
            payReq.setAmount(payAmount);
            payReq.setPaymentMode(mode);
            payReq.setReferenceNumber(billsToSettle.size() > 1 || submission.getAdvanceMonthsCount() > 0 ? baseRef + "-B" + bill.getId() : baseRef);
            payReq.setPaymentDate(LocalDate.now());
            payReq.setBankAccountId(bankAccount.getId());
            payReq.setNotes(String.format("Approved from Resident Submission #%d. %s", submission.getId(),
                    submission.getResidentDescription() != null ? submission.getResidentDescription() : ""));

            RecordPaymentResponse payResp = paymentRecordingService.recordPayment(bill.getId(), null, payReq, auth);
            txIds.add(payResp.getTransactionId());
            lastReceiptNumber = payResp.getReceiptNumber();

            remainingToAllocate = remainingToAllocate.subtract(payAmount).setScale(2, RoundingMode.HALF_UP);
        }

        // 2. Multi-month Advance / Future Maintenance Allocation
        if (remainingToAllocate.compareTo(BigDecimal.ZERO) > 0 || submission.getAdvanceMonthsCount() > 0) {
            int advanceCount = Math.max(submission.getAdvanceMonthsCount(), 1);
            YearMonth nextMonth = YearMonth.now();

            for (int i = 0; i < advanceCount && remainingToAllocate.compareTo(BigDecimal.ZERO) > 0; i++) {
                nextMonth = nextMonth.plusMonths(1);
                String monthStr = nextMonth.format(DateTimeFormatter.ofPattern("uuuu-MM"));

                // Check or create future maintenance bill
                Optional<MaintenanceBill> existingOpt = billRepository.findFirstByRoomIdAndBillingMonthAndChargeType(
                        submission.getRoom().getId(), monthStr, ChargeType.MAINTENANCE);
                MaintenanceBill advanceBill;
                if (existingOpt.isPresent()) {
                    advanceBill = existingOpt.get();
                } else {
                    BigDecimal perMonthAmount = remainingToAllocate.divide(BigDecimal.valueOf(advanceCount - i), 2, RoundingMode.HALF_UP);
                    advanceBill = new MaintenanceBill(
                            submission.getRoom(),
                            monthStr,
                            perMonthAmount,
                            nextMonth.atDay(10)
                    );
                    advanceBill.setChargeType(ChargeType.MAINTENANCE);
                    advanceBill.setStatus(BillStatus.PENDING);
                    advanceBill = billRepository.save(advanceBill);
                }

                BigDecimal billDue = (advanceBill.getAmount() != null ? advanceBill.getAmount() : BigDecimal.ZERO)
                        .subtract(advanceBill.getPaidAmount() != null ? advanceBill.getPaidAmount() : BigDecimal.ZERO);
                BigDecimal payAmount = remainingToAllocate.min(billDue);
                if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    payAmount = remainingToAllocate;
                    advanceBill.setAmount(advanceBill.getAmount().add(payAmount));
                    advanceBill = billRepository.save(advanceBill);
                }

                RecordPaymentRequest payReq = new RecordPaymentRequest();
                payReq.setAmount(payAmount);
                payReq.setPaymentMode(mode);
                payReq.setReferenceNumber(baseRef + "-ADV-" + monthStr);
                payReq.setPaymentDate(LocalDate.now());
                payReq.setBankAccountId(bankAccount.getId());
                payReq.setNotes(String.format("Advance payment for %s approved from Submission #%d", monthStr, submission.getId()));

                RecordPaymentResponse payResp = paymentRecordingService.recordPayment(advanceBill.getId(), null, payReq, auth);
                txIds.add(payResp.getTransactionId());
                lastReceiptNumber = payResp.getReceiptNumber();

                remainingToAllocate = remainingToAllocate.subtract(payAmount).setScale(2, RoundingMode.HALF_UP);
            }
        }

        submission.setStatus("APPROVED");
        submission.setReceivedBankAccount(bankAccount);
        submission.setReviewedByAdmin(admin);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setAdminRemarks(request.getAdminRemarks());
        submission.setResultingTransactionIds(txIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        submission = submissionRepository.save(submission);

        activityLogService.logActivity(admin, "APPROVE_PAYMENT_SUBMISSION", "LEDGER",
                String.format("Approved payment submission #%d for Room %s. Total ₹%s allocated to bank account '%s'. Created transactions: [%s]",
                        submission.getId(), submission.getRoom().getRoomNumber(), submission.getAmount().toPlainString(),
                        bankAccount.getAccountName(), submission.getResultingTransactionIds()));

        ResidentPaymentSubmissionDto resDto = toDto(submission);
        if (request.getReceiptNumber() != null && !request.getReceiptNumber().isBlank()) {
            resDto.setReceiptNumber(request.getReceiptNumber());
        } else if (lastReceiptNumber != null) {
            resDto.setReceiptNumber(lastReceiptNumber);
        }

        if (submission.getSubmittedByUser() != null) {
            try {
                String amountStr = submission.getAmount() != null
                        ? submission.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                        : "0.00";
                String roomNum = submission.getRoom() != null ? submission.getRoom().getRoomNumber() : "";
                StringBuilder msg = new StringBuilder();
                msg.append(String.format("Your payment of ₹%s for room %s was approved.", amountStr, roomNum));
                String receiptNo = resDto.getReceiptNumber();
                if (receiptNo != null && !receiptNo.isBlank()) {
                    msg.append(" Receipt ").append(receiptNo.trim()).append(".");
                }
                notificationService.createNotification(
                        submission.getSubmittedByUser(),
                        "Payment approved",
                        msg.toString(),
                        "/resident/invoices"
                );
            } catch (RuntimeException e) {
                logger.warn("Could not create approval notification: {}", e.getMessage());
            }
        }

        return resDto;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResidentPaymentSubmissionDto rejectSubmission(Long id, RejectPaymentSubmissionRequest request, Authentication auth) {
        User admin = resolveAdminActor(auth);

        if (request.getAdminRemarks() == null || request.getAdminRemarks().trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory");
        }

        ResidentPaymentSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment submission not found with ID: " + id));

        if (!"PENDING".equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException("Submission has already been " + submission.getStatus());
        }

        submission.setStatus("REJECTED");
        submission.setReviewedByAdmin(admin);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setAdminRemarks(request.getAdminRemarks().trim());
        submission = submissionRepository.save(submission);

        activityLogService.logActivity(admin, "REJECT_PAYMENT_SUBMISSION", "LEDGER",
                String.format("Rejected payment submission #%d for Room %s. Remarks: %s",
                        submission.getId(), submission.getRoom().getRoomNumber(), request.getAdminRemarks().trim()));

        if (submission.getSubmittedByUser() != null) {
            try {
                String amountStr = submission.getAmount() != null
                        ? submission.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                        : "0.00";
                String roomNum = submission.getRoom() != null ? submission.getRoom().getRoomNumber() : "";
                String reason = request.getReason() != null ? request.getReason().trim() : "";
                String msg = String.format("Your payment proof of ₹%s for room %s was rejected: %s",
                        amountStr, roomNum, reason);
                notificationService.createNotification(
                        submission.getSubmittedByUser(),
                        "Payment proof rejected",
                        msg,
                        "/resident/invoices"
                );
            } catch (RuntimeException e) {
                logger.warn("Could not create rejection notification: {}", e.getMessage());
            }
        }

        return toDto(submission);
    }

    private User resolveAuthenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    private User resolveAdminActor(Authentication auth) {
        User user = resolveAuthenticatedUser(auth);
        if (!user.getRole().isStaff()) {
            throw new AccessDeniedException("Only Admin or Master Admin can review payment submissions");
        }
        return user;
    }

    public ResidentPaymentSubmissionDto toDto(ResidentPaymentSubmission s) {
        ResidentPaymentSubmissionDto dto = new ResidentPaymentSubmissionDto();
        dto.setId(s.getId());
        if (s.getRoom() != null) {
            dto.setRoomId(s.getRoom().getId());
            dto.setRoomNumber(s.getRoom().getRoomNumber());
        }
        if (s.getSubmittedByUser() != null) {
            dto.setSubmittedByUserId(s.getSubmittedByUser().getId());
            dto.setSubmittedByUserName(s.getSubmittedByUser().getName());
        }
        dto.setAmount(s.getAmount());
        dto.setPaymentMode(s.getPaymentMode());
        dto.setReferenceNumber(s.getReferenceNumber());
        dto.setHasScreenshot(s.getScreenshotData() != null && s.getScreenshotData().length > 0);
        dto.setScreenshotFilename(s.getScreenshotFilename());
        dto.setResidentDescription(s.getResidentDescription());
        dto.setSelectedBillIdsJson(s.getSelectedBillIdsJson());
        dto.setAdvanceMonthsCount(s.getAdvanceMonthsCount());
        dto.setStatus(s.getStatus());
        if (s.getReceivedBankAccount() != null) {
            dto.setReceivedBankAccountId(s.getReceivedBankAccount().getId());
            dto.setReceivedBankAccountName(s.getReceivedBankAccount().getAccountName());
        }
        if (s.getReviewedByAdmin() != null) {
            dto.setReviewedByAdminName(s.getReviewedByAdmin().getName());
        }
        dto.setAdminRemarks(s.getAdminRemarks());
        dto.setResultingTransactionIds(s.getResultingTransactionIds());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setReviewedAt(s.getReviewedAt());
        return dto;
    }

    private static PaymentMode parsePaymentMode(String value) {
        try {
            return PaymentMode.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unknown payment mode '" + value + "'. Allowed: "
                    + java.util.Arrays.toString(PaymentMode.values()));
        }
    }

    /**
     * Every selected bill must exist and belong to the submission's room. Without this, a resident could select
     * another flat's bills and an approval would mark those paid with this resident's money.
     */
    private List<MaintenanceBill> validateSelectedBills(List<Long> billIds, Room room, boolean requirePayable) {
        if (billIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<MaintenanceBill> bills = new ArrayList<>(billRepository.findAllById(billIds));
        if (bills.size() != new java.util.HashSet<>(billIds).size()) {
            throw new IllegalArgumentException("One or more selected bills do not exist");
        }
        for (MaintenanceBill bill : bills) {
            if (bill.getRoom() == null || !bill.getRoom().getId().equals(room.getId())) {
                throw new IllegalArgumentException("Bill #" + bill.getId() + " does not belong to room " + room.getRoomNumber());
            }
            if (requirePayable && bill.getStatus() != BillStatus.PENDING && bill.getStatus() != BillStatus.PARTIALLY_PAID) {
                throw new IllegalArgumentException("Bill #" + bill.getId() + " is already settled (" + bill.getStatus() + ")");
            }
        }
        bills.sort(Comparator.comparing(MaintenanceBill::getBillingMonth));
        return bills;
    }
}
