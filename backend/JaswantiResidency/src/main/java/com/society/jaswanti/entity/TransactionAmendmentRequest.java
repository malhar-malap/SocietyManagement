package com.society.jaswanti.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_amendment_requests")
public class TransactionAmendmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_admin_id", nullable = false)
    private User requestedByAdmin;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // EDIT, REVERSE

    @Column(name = "proposed_payload_json", columnDefinition = "TEXT")
    private String proposedPayloadJson;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_master_admin_id")
    private User reviewedByMasterAdmin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public TransactionAmendmentRequest() {}

    public TransactionAmendmentRequest(Transaction transaction, User requestedByAdmin, String actionType,
                                       String proposedPayloadJson, String reason) {
        this.transaction = transaction;
        this.requestedByAdmin = requestedByAdmin;
        this.actionType = actionType;
        this.proposedPayloadJson = proposedPayloadJson;
        this.reason = reason;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public User getRequestedByAdmin() { return requestedByAdmin; }
    public void setRequestedByAdmin(User requestedByAdmin) { this.requestedByAdmin = requestedByAdmin; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getProposedPayloadJson() { return proposedPayloadJson; }
    public void setProposedPayloadJson(String proposedPayloadJson) { this.proposedPayloadJson = proposedPayloadJson; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getReviewedByMasterAdmin() { return reviewedByMasterAdmin; }
    public void setReviewedByMasterAdmin(User reviewedByMasterAdmin) { this.reviewedByMasterAdmin = reviewedByMasterAdmin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
