package com.society.jaswanti.dto;

import com.society.jaswanti.entity.PaymentMode;
import com.society.jaswanti.entity.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordExpenseRequest {

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Expense amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Expense category is required")
    private TransactionCategory category;

    @NotNull(message = "Bank account is required")
    private Long bankAccountId;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode = PaymentMode.BANK_TRANSFER;

    private String referenceNumber;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    private String payeeName;
    private String notes;
    private String idempotencyKey;

    public RecordExpenseRequest() {}

    public RecordExpenseRequest(BigDecimal amount, TransactionCategory category, Long bankAccountId,
                                PaymentMode paymentMode, String referenceNumber, LocalDate paymentDate,
                                String payeeName, String notes) {
        this.amount = amount;
        this.category = category;
        this.bankAccountId = bankAccountId;
        this.paymentMode = paymentMode;
        this.referenceNumber = referenceNumber;
        this.paymentDate = paymentDate;
        this.payeeName = payeeName;
        this.notes = notes;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
