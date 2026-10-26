-- V20: Widen enum columns on databases that Hibernate built before Flyway was wired up.
--
-- Hibernate ddl-auto=update created ENUM columns with the Java enum values that existed at the time and never widens
-- them afterwards. On such databases newer values are rejected, e.g. maintenance_bills.charge_type lacks 'NOC'
-- (commercial units' companion NOC bills fail with "Data truncated") and users.role could lack 'MASTER_ADMIN'.
--
-- Each statement only touches a column whose type really is ENUM, and keeps its NULL / NOT NULL setting. Fresh
-- Flyway-built databases use VARCHAR for some of these columns and are left as they are. The value lists are the
-- current Java enums (supersets of every list seen in existing databases), so no stored value is invalidated.

-- users.role
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE users MODIFY COLUMN role ENUM(''ADMIN'',''MASTER_ADMIN'',''RESIDENT'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- users.status
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE users MODIFY COLUMN status ENUM(''ACTIVE'',''BLOCKED'',''INACTIVE'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'status' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- maintenance_bills.charge_type
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE maintenance_bills MODIFY COLUMN charge_type ENUM(''MAINTENANCE'',''NOC'',''PENALTY'',''SINKING_FUND'',''SPECIAL_ASSESSMENT'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'maintenance_bills' AND column_name = 'charge_type' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- maintenance_bills.status
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE maintenance_bills MODIFY COLUMN status ENUM(''PAID'',''PAID_CASH'',''PARTIALLY_PAID'',''PENDING'',''REVERSED'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'maintenance_bills' AND column_name = 'status' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- room_residents.relationship_type
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE room_residents MODIFY COLUMN relationship_type ENUM(''OWNER'',''TENANT'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'room_residents' AND column_name = 'relationship_type' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- society_bank_accounts.account_type
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE society_bank_accounts MODIFY COLUMN account_type ENUM(''BANK'',''CASH_LEDGER'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'society_bank_accounts' AND column_name = 'account_type' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
