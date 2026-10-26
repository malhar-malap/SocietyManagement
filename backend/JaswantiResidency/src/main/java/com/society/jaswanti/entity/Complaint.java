package com.society.jaswanti.entity;

import org.hibernate.Length;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private User resident;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "submission_type", nullable = false, length = 30)
    private String submissionType = "COMPLAINT"; // COMPLAINT, QUERY, SUGGESTION, PERMISSION

    @Column(name = "permission_category", length = 50)
    private String permissionCategory; // RENOVATION, INTERIOR_WORK, MOVE_IN_OUT, ADDRESS_PROOF, OTHER

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "admin_response", length = 2000)
    private String adminResponse;

    @Column(name = "image_url")
    private String imageUrl;

    @Lob
    @Column(name = "image_data", length = Length.LONG32) // maps to LONGBLOB on MySQL, not the 255-byte TINYBLOB default
    private byte[] imageData;

    @Column(name = "image_filename")
    private String imageFilename;

    @Column(name = "image_content_type")
    private String imageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    // Constructors
    public Complaint() {}

    public Complaint(User resident, String category, String description) {
        this.resident = resident;
        this.category = category;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getResident() { return resident; }
    public void setResident(User resident) { this.resident = resident; }

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

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
