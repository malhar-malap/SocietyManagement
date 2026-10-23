-- Migration V12: Commercial units, NOC charges, and Share Certificate management
ALTER TABLE rooms
    ADD COLUMN unit_category VARCHAR(30) NOT NULL DEFAULT 'RESIDENTIAL',
    ADD COLUMN noc_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN noc_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN share_certificate_number VARCHAR(100) NULL,
    ADD COLUMN share_certificate_issued_date DATE NULL,
    ADD COLUMN share_certificate_data LONGBLOB NULL,
    ADD COLUMN share_certificate_filename VARCHAR(255) NULL,
    ADD COLUMN share_certificate_content_type VARCHAR(100) NULL;
