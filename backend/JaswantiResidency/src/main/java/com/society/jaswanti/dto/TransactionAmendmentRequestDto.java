package com.society.jaswanti.dto;

import java.time.LocalDateTime;

public class TransactionAmendmentRequestDto {
    private Long id;
    private Long transactionId;
    private String requestedByAdminName;
    private String actionType;
    private String proposedPayloadJson;
    private String reason;
    private String status;
    private String reviewedByMasterAdminName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public TransactionAmendmentRequestDto() {}

    public TransactionAmendmentRequestDto(Long id, Long transactionId, String requestedByAdminName,
                                          String actionType, String proposedPayloadJson, String reason,
                                          String status, String reviewedByMasterAdminName,
                                          LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.requestedByAdminName = requestedByAdminName;
        this.actionType = actionType;
        this.proposedPayloadJson = proposedPayloadJson;
        this.reason = reason;
        this.status = status;
        this.reviewedByMasterAdminName = reviewedByMasterAdminName;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getRequestedByAdminName() { return requestedByAdminName; }
    public void setRequestedByAdminName(String requestedByAdminName) { this.requestedByAdminName = requestedByAdminName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getProposedPayloadJson() { return proposedPayloadJson; }
    public void setProposedPayloadJson(String proposedPayloadJson) { this.proposedPayloadJson = proposedPayloadJson; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedByMasterAdminName() { return reviewedByMasterAdminName; }
    public void setReviewedByMasterAdminName(String reviewedByMasterAdminName) { this.reviewedByMasterAdminName = reviewedByMasterAdminName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
