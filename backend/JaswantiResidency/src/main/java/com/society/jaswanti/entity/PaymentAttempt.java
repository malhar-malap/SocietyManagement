package com.society.jaswanti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private MaintenanceBill bill;

    @Column(name = "razorpay_order_id", nullable = false)
    private String razorpayOrderId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public PaymentAttempt() {}

    public PaymentAttempt(MaintenanceBill bill, String razorpayOrderId, BigDecimal amount) {
        this.bill = bill;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = normalize(amount);
    }

    public PaymentAttempt(MaintenanceBill bill, String razorpayOrderId, Double amount) {
        this(bill, razorpayOrderId, amount != null ? BigDecimal.valueOf(amount) : null);
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public Long getId() { return id; }
    public MaintenanceBill getBill() { return bill; }
    public String getRazorpayOrderId() { return razorpayOrderId; }

    public BigDecimal getAmount() { return amount; }

    @JsonSetter
    public void setAmount(BigDecimal amount) { this.amount = normalize(amount); }

    @JsonIgnore
    public void setAmount(Double amount) { this.amount = amount != null ? normalize(BigDecimal.valueOf(amount)) : null; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
