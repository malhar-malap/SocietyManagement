-- V18: Setup verification codes are now 6 digits, stored as BCrypt hashes, valid for 15 minutes, with at most
-- 5 attempts. Adds the attempt counter and clears any pending plain-text code, so anyone mid-setup simply
-- requests a fresh one.
ALTER TABLE users ADD COLUMN email_verification_attempts INT NOT NULL DEFAULT 0;

UPDATE users
SET email_verification_token = NULL,
    email_verification_expiry = NULL
WHERE email_verification_token IS NOT NULL;
