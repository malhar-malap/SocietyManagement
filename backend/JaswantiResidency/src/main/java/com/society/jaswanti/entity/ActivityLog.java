package com.society.jaswanti.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One entry in the system audit log. Entries are append-only: nothing in the application updates or deletes them.
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILED = "FAILED";
    public static final String OUTCOME_DENIED = "DENIED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null when there is no signed-in actor (e.g. a failed sign-in for an unknown identifier)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Snapshot of the actor at the time of the action
    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String module;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 16)
    private String outcome = OUTCOME_SUCCESS;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "target_label", length = 500)
    private String targetLabel;

    // Constructors
    public ActivityLog() {}

    public ActivityLog(User user, String action, String module, String description, String ipAddress) {
        this.user = user;
        this.action = action;
        this.module = module;
        this.description = description;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetLabel() { return targetLabel; }
    public void setTargetLabel(String targetLabel) { this.targetLabel = targetLabel; }
}
