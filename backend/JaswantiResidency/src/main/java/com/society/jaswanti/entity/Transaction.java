package com.society.jaswanti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = true)
    private MaintenanceBill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private SocietyBankAccount bankAccount;

    @Column(name = "entry_direction", nullable = false, length = 20)
    private String entryDirection = "CREDIT"; // CREDIT, DEBIT

    @Column(name = "category", nullable = false, length = 50)
    private String category = "MAINTENANCE";

    @Column(name = "approved_at")
    private LocalDateTime approvedAt = LocalDateTime.now();

    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Column(name = "payment_mode")
    private String paymentMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "payment_date")
    private java.time.LocalDate paymentDate;

    @Column(length = 1000)
    private String notes;

    @Column(name = "receipt_number", unique = true, length = 64)
    private String receiptNumber;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // Constructors
    public Transaction() {}

    public Transaction(MaintenanceBill bill, BigDecimal amount, String status, String paymentMode) {
        this.bill = bill;
        this.amount = normalize(amount);
        this.status = status;
        this.paymentMode = paymentMode;
    }

    public Transaction(MaintenanceBill bill, Double amount, String status, String paymentMode) {
        this(bill, amount != null ? BigDecimal.valueOf(amount) : null, status, paymentMode);
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : null;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MaintenanceBill getBill() { return bill; }
    public void setBill(MaintenanceBill bill) { this.bill = bill; }

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

    public User getRecordedBy() { return recordedBy; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public java.time.LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(java.time.LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public SocietyBankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(SocietyBankAccount bankAccount) { this.bankAccount = bankAccount; }

    public String getEntryDirection() { return entryDirection; }
    public void setEntryDirection(String entryDirection) { this.entryDirection = entryDirection; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
