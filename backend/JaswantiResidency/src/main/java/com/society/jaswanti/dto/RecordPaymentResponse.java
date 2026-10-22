package com.society.jaswanti.dto;

import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecordPaymentResponse {

    private Long transactionId;
    private String receiptNumber;
    private Long billId;
    private String roomNumber;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private LocalDate paymentDate;
    private BillStatus billStatus;
    private BigDecimal paidAmount;
    private BigDecimal remainingBalance;
    private String notes;
    private String recordedByName;
    private String idempotencyKey;
    private LocalDateTime timestamp;

    public RecordPaymentResponse() {}

    public RecordPaymentResponse(Long transactionId, String receiptNumber, Long billId, String roomNumber,
                                 BigDecimal amount, PaymentMode paymentMode, String referenceNumber,
                                 LocalDate paymentDate, BillStatus billStatus, BigDecimal paidAmount,
                                 BigDecimal remainingBalance, String notes, String recordedByName,
                                 String idempotencyKey, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.receiptNumber = receiptNumber;
        this.billId = billId;
        this.roomNumber = roomNumber;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.referenceNumber = referenceNumber;
        this.paymentDate = paymentDate;
        this.billStatus = billStatus;
        this.paidAmount = paidAmount;
        this.remainingBalance = remainingBalance;
        this.notes = notes;
        this.recordedByName = recordedByName;
        this.idempotencyKey = idempotencyKey;
        this.timestamp = timestamp;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BillStatus getBillStatus() { return billStatus; }
    public void setBillStatus(BillStatus billStatus) { this.billStatus = billStatus; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedByName() { return recordedByName; }
    public void setRecordedByName(String recordedByName) { this.recordedByName = recordedByName; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
