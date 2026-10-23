package com.society.jaswanti.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ResidentPaymentSubmissionDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private Long submittedByUserId;
    private String submittedByUserName;
    private BigDecimal amount;
    private String paymentMode;
    private String referenceNumber;
    private boolean hasScreenshot;
    private String screenshotFilename;
    private String residentDescription;
    private String selectedBillIdsJson;
    private int advanceMonthsCount;
    private String status;
    private Long receivedBankAccountId;
    private String receivedBankAccountName;
    private String reviewedByAdminName;
    private String adminRemarks;
    private String receiptNumber;
    private String resultingTransactionIds;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public ResidentPaymentSubmissionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Long getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(Long submittedByUserId) { this.submittedByUserId = submittedByUserId; }

    public String getSubmittedByUserName() { return submittedByUserName; }
    public void setSubmittedByUserName(String submittedByUserName) { this.submittedByUserName = submittedByUserName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public boolean isHasScreenshot() { return hasScreenshot; }
    public void setHasScreenshot(boolean hasScreenshot) { this.hasScreenshot = hasScreenshot; }

    public String getScreenshotFilename() { return screenshotFilename; }
    public void setScreenshotFilename(String screenshotFilename) { this.screenshotFilename = screenshotFilename; }

    public String getResidentDescription() { return residentDescription; }
    public void setResidentDescription(String residentDescription) { this.residentDescription = residentDescription; }

    public String getSelectedBillIdsJson() { return selectedBillIdsJson; }
    public void setSelectedBillIdsJson(String selectedBillIdsJson) { this.selectedBillIdsJson = selectedBillIdsJson; }

    public int getAdvanceMonthsCount() { return advanceMonthsCount; }
    public void setAdvanceMonthsCount(int advanceMonthsCount) { this.advanceMonthsCount = advanceMonthsCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getReceivedBankAccountId() { return receivedBankAccountId; }
    public void setReceivedBankAccountId(Long receivedBankAccountId) { this.receivedBankAccountId = receivedBankAccountId; }

    public String getReceivedBankAccountName() { return receivedBankAccountName; }
    public void setReceivedBankAccountName(String receivedBankAccountName) { this.receivedBankAccountName = receivedBankAccountName; }

    public String getReviewedByAdminName() { return reviewedByAdminName; }
    public void setReviewedByAdminName(String reviewedByAdminName) { this.reviewedByAdminName = reviewedByAdminName; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public String getRejectionReason() { return adminRemarks; }
    public void setRejectionReason(String rejectionReason) { this.adminRemarks = rejectionReason; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getResultingTransactionIds() { return resultingTransactionIds; }
    public void setResultingTransactionIds(String resultingTransactionIds) { this.resultingTransactionIds = resultingTransactionIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
