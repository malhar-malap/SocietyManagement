-- V11__resident_payment_submissions.sql
CREATE TABLE resident_payment_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    submitted_by_user_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100) NULL,
    screenshot_data LONGBLOB NULL,
    screenshot_filename VARCHAR(255) NULL,
    screenshot_content_type VARCHAR(100) NULL,
    resident_description TEXT NULL,
    selected_bill_ids_json TEXT NULL,
    advance_months_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    received_bank_account_id BIGINT NULL,
    reviewed_by_admin_id BIGINT NULL,
    admin_remarks VARCHAR(500) NULL,
    resulting_transaction_ids VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    CONSTRAINT fk_sub_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_sub_user FOREIGN KEY (submitted_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_sub_bank FOREIGN KEY (received_bank_account_id) REFERENCES society_bank_accounts (id),
    CONSTRAINT fk_sub_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_sub_status ON resident_payment_submissions (status);
CREATE INDEX idx_sub_room ON resident_payment_submissions (room_id);
CREATE INDEX idx_sub_user ON resident_payment_submissions (submitted_by_user_id);
