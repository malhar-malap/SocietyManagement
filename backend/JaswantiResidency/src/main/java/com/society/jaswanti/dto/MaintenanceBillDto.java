package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.society.jaswanti.entity.BillStatus;
import com.society.jaswanti.entity.ChargeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class MaintenanceBillDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private String residentName;
    private String billingMonth;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate issueDate;
    private BigDecimal lateFee;
    private BillStatus status;
    private ChargeType chargeType = ChargeType.MAINTENANCE;
    private BigDecimal paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public MaintenanceBillDto() {}

    public MaintenanceBillDto(Long id, Long roomId, String roomNumber, String residentName, String billingMonth, BigDecimal amount, LocalDate dueDate, LocalDate issueDate, BigDecimal lateFee, BillStatus status) {
        this(id, roomId, roomNumber, residentName, billingMonth, amount, dueDate, issueDate, lateFee, status, ChargeType.MAINTENANCE);
    }

    public MaintenanceBillDto(Long id, Long roomId, String roomNumber, String residentName, String billingMonth, BigDecimal amount, LocalDate dueDate, LocalDate issueDate, BigDecimal lateFee, BillStatus status, ChargeType chargeType) {
        this.id = id;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.residentName = residentName;
        this.billingMonth = billingMonth;
        this.amount = normalize(amount);
        this.dueDate = dueDate;
        this.issueDate = issueDate;
        this.lateFee = normalize(lateFee);
        this.status = status;
        this.chargeType = chargeType != null ? chargeType : ChargeType.MAINTENANCE;
    }

    public MaintenanceBillDto(Long id, Long roomId, String roomNumber, String residentName, String billingMonth, Double amount, LocalDate dueDate, LocalDate issueDate, Double lateFee, BillStatus status) {
        this(id, roomId, roomNumber, residentName, billingMonth,
             amount != null ? BigDecimal.valueOf(amount) : null,
             dueDate, issueDate,
             lateFee != null ? BigDecimal.valueOf(lateFee) : null,
             status, ChargeType.MAINTENANCE);
    }

    public MaintenanceBillDto(Long id, Long roomId, String roomNumber, String residentName, String billingMonth, Double amount, LocalDate dueDate, LocalDate issueDate, Double lateFee, BillStatus status, ChargeType chargeType) {
        this(id, roomId, roomNumber, residentName, billingMonth,
             amount != null ? BigDecimal.valueOf(amount) : null,
             dueDate, issueDate,
             lateFee != null ? BigDecimal.valueOf(lateFee) : null,
             status, chargeType);
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
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
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public BigDecimal getPaidAmount() { return paidAmount; }

    @JsonSetter
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = normalize(paidAmount); }

    @JsonIgnore
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount != null ? normalize(BigDecimal.valueOf(paidAmount)) : null; }

    public BigDecimal getBalance() { return balance; }

    @JsonSetter
    public void setBalance(BigDecimal balance) { this.balance = normalize(balance); }

    @JsonIgnore
    public void setBalance(Double balance) { this.balance = balance != null ? normalize(BigDecimal.valueOf(balance)) : null; }
}
