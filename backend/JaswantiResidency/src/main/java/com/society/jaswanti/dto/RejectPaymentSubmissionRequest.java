package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class RejectPaymentSubmissionRequest {

    @NotBlank(message = "Rejection reason is mandatory")
    @JsonAlias({"reason", "adminRemarks"})
    private String reason;

    public RejectPaymentSubmissionRequest() {}

    public RejectPaymentSubmissionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAdminRemarks() { return reason; }
    public void setAdminRemarks(String adminRemarks) { this.reason = adminRemarks; }
}
