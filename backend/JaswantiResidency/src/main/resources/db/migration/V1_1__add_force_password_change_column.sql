-- V1.1: Add force_password_change column that V1 omitted but V2 requires.
-- Cannot edit V1 (already applied), so this runs between V1 and V2 on fresh databases.
-- On existing databases this is skipped by the baseline.
ALTER TABLE users ADD COLUMN force_password_change BIT(1) NOT NULL DEFAULT 1;
