-- V10__bank_accounts_and_ledger_lock.sql
-- 1. Create society_bank_accounts table
CREATE TABLE society_bank_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    ifsc_code VARCHAR(20),
    account_type VARCHAR(20) NOT NULL, -- BANK, CASH_LEDGER
    opening_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Seed initial default bank accounts if none exist
INSERT INTO society_bank_accounts (account_name, bank_name, account_number, ifsc_code, account_type, opening_balance, current_balance, is_active)
VALUES ('Main Maintenance Bank A/c', 'HDFC Bank', '50100456789123', 'HDFC0001234', 'BANK', 0.00, 0.00, TRUE);

INSERT INTO society_bank_accounts (account_name, bank_name, account_number, ifsc_code, account_type, opening_balance, current_balance, is_active)
VALUES ('Petty Cash Ledger', NULL, NULL, NULL, 'CASH_LEDGER', 0.00, 0.00, TRUE);

-- 3. Modify transactions table
-- Make bill_id nullable for non-bill expense/wages transactions
ALTER TABLE transactions MODIFY bill_id BIGINT NULL;

-- Add bank_account_id, entry_direction, category, and approved_at columns
ALTER TABLE transactions
    ADD COLUMN bank_account_id BIGINT NULL,
    ADD COLUMN entry_direction VARCHAR(20) NOT NULL DEFAULT 'CREDIT',
    ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'MAINTENANCE',
    ADD COLUMN approved_at TIMESTAMP NULL;

-- Add foreign key constraint for bank_account_id
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_bank_account FOREIGN KEY (bank_account_id) REFERENCES society_bank_accounts (id);

-- Backfill approved_at with timestamp for existing transactions
UPDATE transactions SET approved_at = timestamp WHERE approved_at IS NULL;

-- Backfill existing cash transactions with Petty Cash account, and others with Main Bank account
UPDATE transactions SET bank_account_id = (SELECT id FROM society_bank_accounts WHERE account_type = 'CASH_LEDGER' LIMIT 1)
WHERE payment_mode = 'CASH' AND bank_account_id IS NULL;

UPDATE transactions SET bank_account_id = (SELECT id FROM society_bank_accounts WHERE account_type = 'BANK' LIMIT 1)
WHERE bank_account_id IS NULL;

-- 4. Create transaction_amendment_requests table for 24-hour edit lock queue
CREATE TABLE transaction_amendment_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    requested_by_admin_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL, -- EDIT, REVERSE
    proposed_payload_json TEXT,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewed_by_master_admin_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_amend_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_amend_requested_by FOREIGN KEY (requested_by_admin_id) REFERENCES users (id),
    CONSTRAINT fk_amend_reviewed_by FOREIGN KEY (reviewed_by_master_admin_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_amend_status ON transaction_amendment_requests (status);
