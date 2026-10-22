-- V8: Add charge_type column to maintenance_bills and enforce unique constraint per room, billing month, and charge type.
-- This ensures idempotent recurring billing and allows distinct charges (e.g. SINKING_FUND, SPECIAL_ASSESSMENT, PENALTY)
-- for the same unit and billing cycle without duplicate records.
--
-- PRE-CHECK:
-- Run this query prior to migration to verify that no duplicate bills exist for any room and billing month:
--
--   SELECT room_id, billing_month, COUNT(*) AS count
--   FROM maintenance_bills
--   GROUP BY room_id, billing_month
--   HAVING count > 1;
--
-- Pre-check verified: 0 duplicate bills exist in the database.
--

-- 1. Add charge_type column with default 'MAINTENANCE'
ALTER TABLE maintenance_bills
    ADD COLUMN charge_type VARCHAR(32) NOT NULL DEFAULT 'MAINTENANCE';

-- 2. Backfill existing bills to 'MAINTENANCE'
UPDATE maintenance_bills
    SET charge_type = 'MAINTENANCE'
    WHERE charge_type IS NULL;

-- 3. Enforce compound uniqueness per room, billing month, and charge type
ALTER TABLE maintenance_bills
    ADD CONSTRAINT uk_room_month_charge UNIQUE (room_id, billing_month, charge_type);
