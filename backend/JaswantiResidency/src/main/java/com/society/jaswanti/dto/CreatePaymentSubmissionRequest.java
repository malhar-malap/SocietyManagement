package com.society.jaswanti.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CreatePaymentSubmissionRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // UPI_MANUAL, BANK_TRANSFER, CASH

    private String referenceNumber;
    private String residentDescription;
    private List<Long> selectedBillIds;
    private int advanceMonthsCount = 0;

    public CreatePaymentSubmissionRequest() {}

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getResidentDescription() { return residentDescription; }
    public void setResidentDescription(String residentDescription) { this.residentDescription = residentDescription; }

    public List<Long> getSelectedBillIds() { return selectedBillIds; }
    public void setSelectedBillIds(List<Long> selectedBillIds) { this.selectedBillIds = selectedBillIds; }

    public int getAdvanceMonthsCount() { return advanceMonthsCount; }
    public void setAdvanceMonthsCount(int advanceMonthsCount) { this.advanceMonthsCount = advanceMonthsCount; }
}
