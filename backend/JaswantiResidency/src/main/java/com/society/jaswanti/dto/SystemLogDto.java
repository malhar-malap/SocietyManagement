package com.society.jaswanti.dto;

import com.society.jaswanti.entity.ActivityLog;

import java.time.LocalDateTime;

/** What the System Logs page shows for one entry. Deliberately excludes the User entity and its sensitive fields. */
public class SystemLogDto {

    private Long id;
    private LocalDateTime timestamp;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String module;
    private String outcome;
    private String description;
    private String targetType;
    private String targetId;
    private String targetLabel;
    private String httpMethod;
    private String requestPath;
    private String ipAddress;

    public static SystemLogDto from(ActivityLog log) {
        SystemLogDto dto = new SystemLogDto();
        dto.id = log.getId();
        dto.timestamp = log.getTimestamp();
        dto.actorId = log.getUser() != null ? log.getUser().getId() : null;
        dto.actorName = log.getActorName();
        dto.actorEmail = log.getActorEmail();
        dto.actorRole = log.getActorRole();
        dto.action = log.getAction();
        dto.module = log.getModule();
        dto.outcome = log.getOutcome();
        dto.description = log.getDescription();
        dto.targetType = log.getTargetType();
        dto.targetId = log.getTargetId();
        dto.targetLabel = log.getTargetLabel();
        dto.httpMethod = log.getHttpMethod();
        dto.requestPath = log.getRequestPath();
        dto.ipAddress = log.getIpAddress();
        return dto;
    }

    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getActorEmail() { return actorEmail; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getModule() { return module; }
    public String getOutcome() { return outcome; }
    public String getDescription() { return description; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getTargetLabel() { return targetLabel; }
    public String getHttpMethod() { return httpMethod; }
    public String getRequestPath() { return requestPath; }
    public String getIpAddress() { return ipAddress; }
}
