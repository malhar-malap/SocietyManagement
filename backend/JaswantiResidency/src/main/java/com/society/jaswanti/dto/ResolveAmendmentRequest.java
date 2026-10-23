package com.society.jaswanti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResolveAmendmentRequest {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Action must be APPROVE or REJECT")
    private String action; // APPROVE, REJECT

    private String reviewNotes;

    public ResolveAmendmentRequest() {}

    public ResolveAmendmentRequest(String action, String reviewNotes) {
        this.action = action;
        this.reviewNotes = reviewNotes;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
