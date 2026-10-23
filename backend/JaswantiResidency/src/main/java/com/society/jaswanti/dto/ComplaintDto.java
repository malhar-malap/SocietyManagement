package com.society.jaswanti.dto;

import com.society.jaswanti.entity.ComplaintStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ComplaintDto {
    private Long id;
    private Long residentId;
    private String residentName;
    private String roomNumber;
    private String category;
    private String title;
    private String description;
    private String submissionType; // COMPLAINT, QUERY, SUGGESTION, PERMISSION
    private String permissionCategory;
    private LocalDate startDate;
    private LocalDate endDate;
    private String adminResponse;
    private String imageUrl;
    private boolean hasAttachment;
    private ComplaintStatus status;
    private LocalDateTime createdDate;

    public ComplaintDto() {}

    public ComplaintDto(Long id, Long residentId, String roomNumber, String category, String title, String description, String imageUrl, ComplaintStatus status, LocalDateTime createdDate) {
        this.id = id;
        this.residentId = residentId;
        this.roomNumber = roomNumber;
        this.category = category;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdDate = createdDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getResidentId() { return residentId; }
    public void setResidentId(Long residentId) { this.residentId = residentId; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubmissionType() { return submissionType; }
    public void setSubmissionType(String submissionType) { this.submissionType = submissionType; }

    public String getPermissionCategory() { return permissionCategory; }
    public void setPermissionCategory(String permissionCategory) { this.permissionCategory = permissionCategory; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isHasAttachment() { return hasAttachment; }
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
