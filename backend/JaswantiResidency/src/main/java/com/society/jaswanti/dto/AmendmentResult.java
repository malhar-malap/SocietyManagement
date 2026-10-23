package com.society.jaswanti.dto;

public class AmendmentResult {
    private boolean pendingApproval;
    private String message;
    private Long amendmentRequestId;

    public AmendmentResult() {}

    public AmendmentResult(boolean pendingApproval, String message, Long amendmentRequestId) {
        this.pendingApproval = pendingApproval;
        this.message = message;
        this.amendmentRequestId = amendmentRequestId;
    }

    public boolean isPendingApproval() { return pendingApproval; }
    public void setPendingApproval(boolean pendingApproval) { this.pendingApproval = pendingApproval; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getAmendmentRequestId() { return amendmentRequestId; }
    public void setAmendmentRequestId(Long amendmentRequestId) { this.amendmentRequestId = amendmentRequestId; }
}
