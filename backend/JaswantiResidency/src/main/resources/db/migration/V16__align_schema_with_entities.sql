-- V16: Bring the Flyway-built schema in line with the JPA entities so the app can start with ddl-auto=validate.
--
-- Until Flyway was wired up (step 4), every database was built by Hibernate ddl-auto=update, so some tables and
-- columns only ever existed because Hibernate created them. This migration must therefore run safely on BOTH:
--   * fresh databases built by V1..V15, where these objects are missing or have the wrong type, and
--   * existing Hibernate-built databases (baselined at 14), where most of them already exist.
-- Every statement below is either IF NOT EXISTS, guarded through information_schema, or an idempotent MODIFY.

-- 1. Tables that only Hibernate ever created
CREATE TABLE IF NOT EXISTS activity_logs (
    id bigint NOT NULL AUTO_INCREMENT,
    action varchar(255) NOT NULL,
    description varchar(2000) DEFAULT NULL,
    ip_address varchar(255) DEFAULT NULL,
    module varchar(255) NOT NULL,
    timestamp datetime(6) NOT NULL,
    user_id bigint NOT NULL,
    PRIMARY KEY (id),
    KEY idx_activity_logs_user (user_id),
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_notifications (
    id bigint NOT NULL AUTO_INCREMENT,
    created_at datetime(6) NOT NULL,
    is_read bit(1) NOT NULL,
    link varchar(255) DEFAULT NULL,
    message varchar(1000) NOT NULL,
    title varchar(255) NOT NULL,
    user_id bigint DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_system_notifications_user (user_id),
    CONSTRAINT fk_system_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- 2. users columns used by profile setup / email verification (MySQL has no ADD COLUMN IF NOT EXISTS)
SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE users ADD COLUMN pending_email varchar(255) NULL', 'DO 0')
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'pending_email');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE users ADD COLUMN email_verification_token varchar(255) NULL', 'DO 0')
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email_verification_token');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE users ADD COLUMN email_verification_expiry datetime(6) NULL', 'DO 0')
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email_verification_expiry');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. complaints.status: V1 only allowed OPEN/IN_PROGRESS/RESOLVED, but queries/suggestions are APPROVED or REJECTED
ALTER TABLE complaints
    MODIFY COLUMN status enum ('APPROVED','IN_PROGRESS','OPEN','REJECTED','RESOLVED') NOT NULL;

-- 4. Uploaded files: Hibernate's default for @Lob byte[] on MySQL is TINYBLOB (255 bytes), which cannot hold a real
--    screenshot, photo or PDF. Widen to LONGBLOB everywhere (the entities now declare a matching length).
ALTER TABLE resident_payment_submissions MODIFY COLUMN screenshot_data LONGBLOB NULL;
ALTER TABLE complaints MODIFY COLUMN image_data LONGBLOB NULL;
ALTER TABLE rooms MODIFY COLUMN share_certificate_data LONGBLOB NULL;
