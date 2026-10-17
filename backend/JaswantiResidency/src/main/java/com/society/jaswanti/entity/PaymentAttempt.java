package com.society.jaswanti.entity;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private Double amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public PaymentAttempt() {}

    public PaymentAttempt(MaintenanceBill bill, String razorpayOrderId, Double amount) {
        this.bill = bill;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public MaintenanceBill getBill() { return bill; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public Double getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
