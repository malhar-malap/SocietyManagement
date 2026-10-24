-- V17: Turn activity_logs into the system audit log shown on the Master Admin "System Logs" page.
--
-- * user_id becomes nullable: some events have no signed-in actor (e.g. a failed sign-in for an unknown identifier).
-- * actor_* columns snapshot who acted at the time, so entries stay readable even if the user is renamed later.
-- * outcome records SUCCESS / FAILED / DENIED, so refused and failed attempts are audited too.
-- * http_method / request_path / target_* identify exactly what was acted on.
-- V16 guarantees the table exists with the same shape on fresh and existing databases, so plain DDL is safe here.
ALTER TABLE activity_logs
    MODIFY COLUMN user_id bigint NULL,
    ADD COLUMN actor_name varchar(255) NULL,
    ADD COLUMN actor_email varchar(255) NULL,
    ADD COLUMN actor_role varchar(32) NULL,
    ADD COLUMN outcome varchar(16) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN http_method varchar(10) NULL,
    ADD COLUMN request_path varchar(500) NULL,
    ADD COLUMN target_type varchar(64) NULL,
    ADD COLUMN target_id varchar(64) NULL,
    ADD COLUMN target_label varchar(500) NULL;

CREATE INDEX idx_activity_logs_timestamp ON activity_logs (timestamp);
CREATE INDEX idx_activity_logs_module_action ON activity_logs (module, action);

-- Backfill the actor snapshot for entries written before this migration
UPDATE activity_logs l
JOIN users u ON u.id = l.user_id
SET l.actor_name = u.name,
    l.actor_email = u.email,
    l.actor_role = u.role
WHERE l.actor_email IS NULL;
