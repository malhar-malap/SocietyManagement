package com.society.jaswanti.dto;

import java.time.LocalDateTime;

public class SystemNotificationDto {
    private Long id;
    private String title;
    private String message;
    private String link;
    private LocalDateTime createdAt;
    private boolean read;

    public SystemNotificationDto(Long id, String title, String message, String link, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.link = link;
        this.createdAt = createdAt;
        this.read = read;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
}
