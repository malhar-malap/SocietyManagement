-- Migration V13: Helpdesk extensions for Queries, Suggestions, Permissions, and Admin Responses
ALTER TABLE complaints
    ADD COLUMN submission_type VARCHAR(30) NOT NULL DEFAULT 'COMPLAINT',
    ADD COLUMN permission_category VARCHAR(50) NULL,
    ADD COLUMN start_date DATE NULL,
    ADD COLUMN end_date DATE NULL,
    ADD COLUMN admin_response TEXT NULL,
    ADD COLUMN image_data LONGBLOB NULL,
    ADD COLUMN image_filename VARCHAR(255) NULL,
    ADD COLUMN image_content_type VARCHAR(100) NULL;
