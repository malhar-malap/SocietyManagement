package com.society.jaswanti.dto;

import com.society.jaswanti.entity.BillStatus;
import java.time.LocalDate;

public class MaintenanceBillDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private String residentName;
    private String billingMonth;
    private Double amount;
    private LocalDate dueDate;
    private LocalDate issueDate;
    private Double lateFee;
    private BillStatus status;

    public MaintenanceBillDto() {}

    public MaintenanceBillDto(Long id, Long roomId, String roomNumber, String residentName, String billingMonth, Double amount, LocalDate dueDate, LocalDate issueDate, Double lateFee, BillStatus status) {
        this.id = id;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.residentName = residentName;
        this.billingMonth = billingMonth;
        this.amount = amount;
        this.dueDate = dueDate;
        this.issueDate = issueDate;
        this.lateFee = lateFee;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Double getLateFee() { return lateFee; }
    public void setLateFee(Double lateFee) { this.lateFee = lateFee; }
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
}
