package com.society.jaswanti.dto;

import java.time.LocalDate;

public class AssignResidentRequest {
    private Long residentId;
    private String relationshipType; // OWNER or TENANT
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean primaryContact;

    public AssignResidentRequest() {}

    public Long getResidentId() { return residentId; }
    public void setResidentId(Long residentId) { this.residentId = residentId; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(Boolean primaryContact) { this.primaryContact = primaryContact; }
}
