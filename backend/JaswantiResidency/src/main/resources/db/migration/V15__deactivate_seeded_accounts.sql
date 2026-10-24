-- V15: Deactivate seeded default accounts created by V2.
-- Covers admin@jaswanti.com and all resident%@jaswanti.com accounts
-- that have not completed the verification process (force_password_change=1 AND email_verified=0).
-- Accounts that completed setup (changed password, verified email) are left untouched.
-- No rows are deleted; foreign keys to rooms, bills, and logs remain intact.
-- token_version is bumped to invalidate any outstanding JWT tokens for these accounts.

-- 1. Deactivate the seeded admin account
UPDATE users
SET is_active = 0, status = 'INACTIVE', token_version = token_version + 1
WHERE email = 'admin@jaswanti.com';

-- 2. Deactivate seeded resident accounts still on default credentials
UPDATE users
SET is_active = 0, status = 'INACTIVE', token_version = token_version + 1
WHERE email LIKE 'resident%@jaswanti.com'
  AND force_password_change = 1
  AND email_verified = 0;
