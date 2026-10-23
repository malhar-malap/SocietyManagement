package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public class ApprovePaymentSubmissionRequest {

    @NotNull(message = "Bank Account ID is required")
    @JsonAlias({"bankAccountId", "receivedBankAccountId"})
    private Long bankAccountId;

    @JsonAlias({"adminRemarks", "notes"})
    private String notes;

    private String receiptNumber;

    public ApprovePaymentSubmissionRequest() {}

    public ApprovePaymentSubmissionRequest(Long bankAccountId, String notes) {
        this.bankAccountId = bankAccountId;
        this.notes = notes;
    }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public Long getReceivedBankAccountId() { return bankAccountId; }
    public void setReceivedBankAccountId(Long receivedBankAccountId) { this.bankAccountId = receivedBankAccountId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAdminRemarks() { return notes; }
    public void setAdminRemarks(String adminRemarks) { this.notes = adminRemarks; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
}
