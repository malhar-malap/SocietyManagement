-- V7: Migrate monetary columns from floating-point (DOUBLE/FLOAT) to fixed-precision DECIMAL(12, 2).
-- This ensures exact currency representation (up to 9,999,999,999.99 with scale 2) and eliminates IEEE 754 rounding artifacts.
--
-- PRE-CHECK:
-- Before running this migration on production databases with high data volume,
-- operators may run the following queries to verify that no existing records exceed the DECIMAL(12, 2) range:
--
--   SELECT id, amount FROM maintenance_bills WHERE amount > 9999999999.99 OR amount < -9999999999.99;
--   SELECT id, late_fee FROM maintenance_bills WHERE late_fee > 9999999999.99 OR late_fee < -9999999999.99;
--   SELECT id, amount FROM transactions WHERE amount > 9999999999.99 OR amount < -9999999999.99;
--   SELECT id, amount FROM payment_attempts WHERE amount > 9999999999.99 OR amount < -9999999999.99;
--

-- 1. Modify maintenance_bills monetary columns
ALTER TABLE maintenance_bills
    MODIFY COLUMN amount DECIMAL(12, 2) NOT NULL,
    MODIFY COLUMN late_fee DECIMAL(12, 2) DEFAULT 0.00;

-- 2. Modify transactions monetary columns
ALTER TABLE transactions
    MODIFY COLUMN amount DECIMAL(12, 2) NOT NULL;

-- 3. Modify payment_attempts monetary columns (ensure table exists for fresh flyway runs)
CREATE TABLE IF NOT EXISTS payment_attempts (
    id bigint not null auto_increment,
    bill_id bigint not null,
    razorpay_order_id varchar(255) not null,
    amount decimal(12, 2) not null,
    created_at datetime(6),
    primary key (id),
    constraint fk_payment_attempts_bill foreign key (bill_id) references maintenance_bills (id)
) engine=InnoDB;

ALTER TABLE payment_attempts
    MODIFY COLUMN amount DECIMAL(12, 2) NOT NULL;
