-- Add password reset OTP columns to users table
ALTER TABLE users ADD COLUMN password_reset_otp_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN password_reset_otp_expiry DATETIME(6) NULL;
ALTER TABLE users ADD COLUMN password_reset_otp_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN password_reset_otp_last_sent_at DATETIME(6) NULL;
