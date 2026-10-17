package com.society.jaswanti.dto;

import java.time.LocalDateTime;

public class AnnouncementDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime expiryDate;
    private Long createdById;
    private String createdByName;

    public AnnouncementDto() {}

    public AnnouncementDto(Long id, String title, String description, LocalDateTime createdDate, LocalDateTime expiryDate, Long createdById, String createdByName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdDate = createdDate;
        this.expiryDate = expiryDate;
        this.createdById = createdById;
        this.createdByName = createdByName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
}
