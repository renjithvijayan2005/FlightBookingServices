-- ============================================================
-- V12__fix_bookings_schema.sql
-- Adds missing columns to bookings table and fixes constraints
-- All changes are conditional (idempotent via information_schema)
-- ============================================================

SET @db = DATABASE();

-- 1. Add booking_reference (required for every booking)
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'booking_reference');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN booking_reference VARCHAR(20)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Add amadeus_order_id (nullable — only for Amadeus bookings)
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'amadeus_order_id');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN amadeus_order_id VARCHAR(100)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Add num_passengers (defaults to 1)
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'num_passengers');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN num_passengers INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Add total_amount
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'total_amount');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN total_amount DECIMAL(10,2)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Add created_at
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'created_at');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN created_at DATETIME(6)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. Add updated_at
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'updated_at');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE bookings ADD COLUMN updated_at DATETIME(6)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. Make flight_id nullable (Amadeus bookings have no local flight row)
SET @nullable = (SELECT IS_NULLABLE FROM information_schema.columns WHERE table_schema = @db AND table_name = 'bookings' AND column_name = 'flight_id');
SET @ddl = IF(@nullable = 'NO', 'ALTER TABLE bookings MODIFY COLUMN flight_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8. Add UNIQUE index on booking_reference (skip if already exists)
SET @idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @db AND table_name = 'bookings' AND index_name = 'uk_booking_reference');
SET @ddl = IF(@idx = 0, 'ALTER TABLE bookings ADD UNIQUE INDEX uk_booking_reference (booking_reference)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
