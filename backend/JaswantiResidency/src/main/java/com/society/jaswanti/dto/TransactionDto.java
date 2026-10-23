package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class TransactionDto {
    private Long id;
    private Long billId;
    private String residentName;
    private String roomNumber;
    private String gatewayPaymentId;
    private String paymentMode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
    private String billingMonth;
    private String referenceNumber;
    private java.time.LocalDate paymentDate;
    private String receiptNumber;
    private String notes;
    private String recordedByName;
    private String idempotencyKey;
    private String entryDirection = "CREDIT";
    private String category = "MAINTENANCE";
    private Long bankAccountId;
    private String bankAccountName;
    private LocalDateTime approvedAt;
    private boolean locked;

    public TransactionDto() {}

    public TransactionDto(Long id, Long billId, String residentName, String roomNumber, String gatewayPaymentId, String paymentMode, BigDecimal amount, String status, LocalDateTime timestamp, String billingMonth) {
        this.id = id;
        this.billId = billId;
        this.residentName = residentName;
        this.roomNumber = roomNumber;
        this.gatewayPaymentId = gatewayPaymentId;
        this.paymentMode = paymentMode;
        this.amount = normalize(amount);
        this.status = status;
        this.timestamp = timestamp;
        this.billingMonth = billingMonth;
    }

    public TransactionDto(Long id, Long billId, String residentName, String roomNumber, String gatewayPaymentId, String paymentMode, Double amount, String status, LocalDateTime timestamp, String billingMonth) {
        this(id, billId, residentName, roomNumber, gatewayPaymentId, paymentMode,
             amount != null ? BigDecimal.valueOf(amount) : null,
             status, timestamp, billingMonth);
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public BigDecimal getAmount() { return amount; }

    @JsonSetter
    public void setAmount(BigDecimal amount) { this.amount = normalize(amount); }

    @JsonIgnore
    public void setAmount(Double amount) { this.amount = amount != null ? normalize(BigDecimal.valueOf(amount)) : null; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public java.time.LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(java.time.LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedByName() { return recordedByName; }
    public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getEntryDirection() { return entryDirection; }
    public void setEntryDirection(String entryDirection) { this.entryDirection = entryDirection; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
