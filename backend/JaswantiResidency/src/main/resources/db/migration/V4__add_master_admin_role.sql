-- Add the privileged role without changing or rewriting any prior migration.
ALTER TABLE users
    MODIFY COLUMN role ENUM('ADMIN', 'MASTER_ADMIN', 'RESIDENT') NOT NULL;
