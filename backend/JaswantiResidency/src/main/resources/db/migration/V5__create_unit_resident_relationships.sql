-- Phase 2: Create unit-resident relationships table to support multiple owners/tenants per unit and multiple units per person over time
CREATE TABLE room_residents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    resident_id BIGINT NOT NULL,
    relationship_type ENUM('OWNER', 'TENANT') NOT NULL DEFAULT 'OWNER',
    start_date DATE NULL,
    end_date DATE NULL,
    is_active BIT(1) NOT NULL DEFAULT 1,
    is_primary_contact BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_room_residents_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_room_residents_user FOREIGN KEY (resident_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- Backfill existing rooms.resident_id into active OWNER relationships
INSERT INTO room_residents (room_id, resident_id, relationship_type, start_date, end_date, is_active, is_primary_contact, created_at)
SELECT r.id, r.resident_id, 'OWNER', '2026-01-01', NULL, 1, 1, NOW()
FROM rooms r
WHERE r.resident_id IS NOT NULL;
