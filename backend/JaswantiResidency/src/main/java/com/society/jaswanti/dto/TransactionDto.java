package com.society.jaswanti.dto;

import java.time.LocalDateTime;

public class TransactionDto {
    private Long id;
    private Long billId;
    private String residentName;
    private String roomNumber;
    private String gatewayPaymentId;
    private String paymentMode;
    private Double amount;
    private String status;
    private LocalDateTime timestamp;
    private String billingMonth;

    public TransactionDto() {}

    public TransactionDto(Long id, Long billId, String residentName, String roomNumber, String gatewayPaymentId, String paymentMode, Double amount, String status, LocalDateTime timestamp, String billingMonth) {
        this.id = id;
        this.billId = billId;
        this.residentName = residentName;
        this.roomNumber = roomNumber;
        this.gatewayPaymentId = gatewayPaymentId;
        this.paymentMode = paymentMode;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.billingMonth = billingMonth;
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
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }
}
