-- V19: Ensure users.role allows MASTER_ADMIN on databases baselined at V14 where V4 was skipped
ALTER TABLE users
    MODIFY COLUMN role ENUM('ADMIN', 'MASTER_ADMIN', 'RESIDENT') NOT NULL;
