-- V21: Allow the OFFICE_ASSISTANT staff role (admin access to society, residents and finance; no helpdesk or announcements).
--
-- Like V20, only a column whose type really is ENUM is changed, keeping its NULL / NOT NULL setting; VARCHAR columns
-- already accept the new value.
SET @ddl = (SELECT IF(COUNT(*) = 0, 'DO 0',
                CONCAT('ALTER TABLE users MODIFY COLUMN role ENUM(''ADMIN'',''MASTER_ADMIN'',''OFFICE_ASSISTANT'',''RESIDENT'') ',
                       IF(MAX(is_nullable) = 'NO', 'NOT NULL', 'NULL')))
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role' AND data_type = 'enum');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
