package com.society.jaswanti.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_residents")
public class RoomResident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private User resident;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType = RelationshipType.OWNER;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_primary_contact", nullable = false)
    private boolean isPrimaryContact = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RoomResident() {}

    public RoomResident(Room room, User resident, RelationshipType relationshipType, LocalDate startDate, boolean isPrimaryContact) {
        this.room = room;
        this.resident = resident;
        this.relationshipType = relationshipType;
        this.startDate = startDate;
        this.isPrimaryContact = isPrimaryContact;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getResident() { return resident; }
    public void setResident(User resident) { this.resident = resident; }

    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isPrimaryContact() { return isPrimaryContact; }
    public void setPrimaryContact(boolean primaryContact) { isPrimaryContact = primaryContact; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
