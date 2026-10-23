package com.society.jaswanti.dto;

import jakarta.validation.constraints.NotBlank;

public class ReverseTransactionRequest {

    @NotBlank(message = "Reversal reason is required")
    private String reason;

    public ReverseTransactionRequest() {}

    public ReverseTransactionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
