package com.society.jaswanti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_bills", uniqueConstraints = {
    @UniqueConstraint(name = "uk_room_month_charge", columnNames = {"room_id", "billing_month", "charge_type"})
})
public class MaintenanceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "billing_month", nullable = false)
    private String billingMonth; // e.g., "July 2026"

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false)
    private ChargeType chargeType = ChargeType.MAINTENANCE;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "late_fee", precision = 12, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "paid_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BillStatus status = BillStatus.PENDING;

    // Constructors
    public MaintenanceBill() {}

    public MaintenanceBill(Room room, String billingMonth, BigDecimal amount, LocalDate dueDate) {
        this(room, billingMonth, amount, dueDate, ChargeType.MAINTENANCE);
    }

    public MaintenanceBill(Room room, String billingMonth, BigDecimal amount, LocalDate dueDate, ChargeType chargeType) {
        this.room = room;
        this.billingMonth = billingMonth;
        this.amount = normalize(amount);
        this.dueDate = dueDate;
        this.chargeType = chargeType != null ? chargeType : ChargeType.MAINTENANCE;
    }

    public MaintenanceBill(Room room, String billingMonth, Double amount, LocalDate dueDate) {
        this(room, billingMonth, amount != null ? BigDecimal.valueOf(amount) : null, dueDate, ChargeType.MAINTENANCE);
    }

    public MaintenanceBill(Room room, String billingMonth, Double amount, LocalDate dueDate, ChargeType chargeType) {
        this(room, billingMonth, amount != null ? BigDecimal.valueOf(amount) : null, dueDate, chargeType);
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : null;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

    public ChargeType getChargeType() { return chargeType; }
    public void setChargeType(ChargeType chargeType) { this.chargeType = chargeType != null ? chargeType : ChargeType.MAINTENANCE; }

    public BigDecimal getAmount() { return amount; }

    @JsonSetter
    public void setAmount(BigDecimal amount) { this.amount = normalize(amount); }

    @JsonIgnore
    public void setAmount(Double amount) { this.amount = amount != null ? normalize(BigDecimal.valueOf(amount)) : null; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getLateFee() { return lateFee; }

    @JsonSetter
    public void setLateFee(BigDecimal lateFee) { this.lateFee = normalize(lateFee); }

    @JsonIgnore
    public void setLateFee(Double lateFee) { this.lateFee = lateFee != null ? normalize(BigDecimal.valueOf(lateFee)) : null; }

    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }

    public BigDecimal getPaidAmount() {
        return paidAmount != null ? paidAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    @JsonSetter
    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = normalize(paidAmount);
    }

    @JsonIgnore
    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount != null ? normalize(BigDecimal.valueOf(paidAmount)) : null;
    }

    public BigDecimal getBalance() {
        BigDecimal totalDue = (amount != null ? amount : BigDecimal.ZERO)
                .add(lateFee != null ? lateFee : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = getPaidAmount();
        return totalDue.subtract(paid).setScale(2, RoundingMode.HALF_UP);
    }
}
