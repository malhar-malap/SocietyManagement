-- V9: Schema updates for audited manual payment recording.
-- 1. Convert maintenance_bills.status from ENUM to VARCHAR(32) to support PARTIALLY_PAID and REVERSED statuses.
-- 2. Add paid_amount column to maintenance_bills with backfill for existing paid bills.
-- 3. Add payment metadata columns to transactions (recorded_by_id, reference_number, payment_date, notes, receipt_number, idempotency_key).
-- 4. Create receipt_sequences table for transactional, gap-free receipt number generation.

-- 1. Modify maintenance_bills.status to VARCHAR(32)
ALTER TABLE maintenance_bills
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

-- 2. Add paid_amount to maintenance_bills
ALTER TABLE maintenance_bills
    ADD COLUMN paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- Backfill paid_amount for existing paid bills
UPDATE maintenance_bills
    SET paid_amount = CASE
        WHEN status IN ('PAID', 'PAID_CASH') THEN amount + COALESCE(late_fee, 0.00)
        ELSE 0.00
    END;

-- 3. Add metadata columns to transactions
ALTER TABLE transactions
    ADD COLUMN recorded_by_id BIGINT NULL,
    ADD COLUMN reference_number VARCHAR(255) NULL,
    ADD COLUMN payment_date DATE NULL,
    ADD COLUMN notes VARCHAR(1000) NULL,
    ADD COLUMN receipt_number VARCHAR(64) NULL,
    ADD COLUMN idempotency_key VARCHAR(255) NULL;

-- 4. Add constraints and foreign keys to transactions
ALTER TABLE transactions
    ADD CONSTRAINT uk_transactions_receipt_number UNIQUE (receipt_number);

ALTER TABLE transactions
    ADD CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_recorded_by FOREIGN KEY (recorded_by_id) REFERENCES users (id);

-- 5. Create receipt_sequences table for safe sequence generation
CREATE TABLE IF NOT EXISTS receipt_sequences (
    period VARCHAR(6) NOT NULL,
    next_val BIGINT NOT NULL,
    PRIMARY KEY (period)
) ENGINE=InnoDB;
