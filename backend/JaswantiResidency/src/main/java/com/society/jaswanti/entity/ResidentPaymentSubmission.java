package com.society.jaswanti.entity;

import org.hibernate.Length;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "resident_payment_submissions")
public class ResidentPaymentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private User submittedByUser;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "payment_mode", nullable = false, length = 30)
    private String paymentMode; // UPI_MANUAL, BANK_TRANSFER, CASH

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "screenshot_data", length = Length.LONG32) // maps to LONGBLOB on MySQL, not the 255-byte TINYBLOB default
    private byte[] screenshotData;

    @Column(name = "screenshot_filename")
    private String screenshotFilename;

    @Column(name = "screenshot_content_type", length = 100)
    private String screenshotContentType;

    @Column(name = "resident_description", columnDefinition = "TEXT")
    private String residentDescription;

    @Column(name = "selected_bill_ids_json", columnDefinition = "TEXT")
    private String selectedBillIdsJson;

    @Column(name = "advance_months_count", nullable = false)
    private int advanceMonthsCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_bank_account_id")
    private SocietyBankAccount receivedBankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    @Column(name = "admin_remarks", length = 500)
    private String adminRemarks;

    @Column(name = "resulting_transaction_ids")
    private String resultingTransactionIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public ResidentPaymentSubmission() {}

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getSubmittedByUser() { return submittedByUser; }
    public void setSubmittedByUser(User submittedByUser) { this.submittedByUser = submittedByUser; }

    public BigDecimal getAmount() { return amount; }

    @JsonSetter
    public void setAmount(BigDecimal amount) { this.amount = normalize(amount); }

    @JsonIgnore
    public void setAmount(Double amount) {
        this.amount = amount != null ? normalize(BigDecimal.valueOf(amount)) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public byte[] getScreenshotData() { return screenshotData; }
    public void setScreenshotData(byte[] screenshotData) { this.screenshotData = screenshotData; }

    public String getScreenshotFilename() { return screenshotFilename; }
    public void setScreenshotFilename(String screenshotFilename) { this.screenshotFilename = screenshotFilename; }

    public String getScreenshotContentType() { return screenshotContentType; }
    public void setScreenshotContentType(String screenshotContentType) { this.screenshotContentType = screenshotContentType; }

    public String getResidentDescription() { return residentDescription; }
    public void setResidentDescription(String residentDescription) { this.residentDescription = residentDescription; }

    public String getSelectedBillIdsJson() { return selectedBillIdsJson; }
    public void setSelectedBillIdsJson(String selectedBillIdsJson) { this.selectedBillIdsJson = selectedBillIdsJson; }

    public int getAdvanceMonthsCount() { return advanceMonthsCount; }
    public void setAdvanceMonthsCount(int advanceMonthsCount) { this.advanceMonthsCount = advanceMonthsCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public SocietyBankAccount getReceivedBankAccount() { return receivedBankAccount; }
    public void setReceivedBankAccount(SocietyBankAccount receivedBankAccount) { this.receivedBankAccount = receivedBankAccount; }

    public User getReviewedByAdmin() { return reviewedByAdmin; }
    public void setReviewedByAdmin(User reviewedByAdmin) { this.reviewedByAdmin = reviewedByAdmin; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public String getResultingTransactionIds() { return resultingTransactionIds; }
    public void setResultingTransactionIds(String resultingTransactionIds) { this.resultingTransactionIds = resultingTransactionIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
