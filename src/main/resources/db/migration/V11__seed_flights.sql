-- ============================================================
-- V11__seed_flights.sql
-- 1. Adds airline_code column to airlines (if missing)
-- 2. Seeds airline codes + missing airlines (SQ, QF, CX)
-- 3. Inserts sample flights for end-to-end testing
-- ============================================================

-- Step 1: Add airline_code column if it does not already exist
SET @s = DATABASE();
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @s AND table_name = 'airlines' AND column_name = 'airline_code');
SET @ddl = IF(@cnt = 0, 'ALTER TABLE airlines ADD COLUMN airline_code VARCHAR(10)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Set airline_code for existing airlines (idempotent)
UPDATE airlines SET airline_code = 'EK' WHERE airline_name = 'Emirates' AND (airline_code IS NULL OR airline_code = '');
UPDATE airlines SET airline_code = 'BA' WHERE airline_name = 'British Airways' AND (airline_code IS NULL OR airline_code = '');
UPDATE airlines SET airline_code = 'SQ' WHERE airline_name = 'Singapore Airlines' AND (airline_code IS NULL OR airline_code = '');
UPDATE airlines SET airline_code = 'QF' WHERE airline_name = 'Qantas' AND (airline_code IS NULL OR airline_code = '');
UPDATE airlines SET airline_code = 'CX' WHERE airline_name = 'Cathay Pacific' AND (airline_code IS NULL OR airline_code = '');

-- Step 3: Insert missing airlines (INSERT IGNORE skips duplicates by name)
INSERT INTO airlines (airline_code, airline_name, contact_number, operating_region)
SELECT 'SQ', 'Singapore Airlines', '+6562238888', 'Asia/Global'
WHERE NOT EXISTS (SELECT 1 FROM airlines WHERE airline_name = 'Singapore Airlines');

INSERT INTO airlines (airline_code, airline_name, contact_number, operating_region)
SELECT 'QF', 'Qantas', '+1800227467', 'Australia/Global'
WHERE NOT EXISTS (SELECT 1 FROM airlines WHERE airline_name = 'Qantas');

INSERT INTO airlines (airline_code, airline_name, contact_number, operating_region)
SELECT 'CX', 'Cathay Pacific', '+85227473333', 'Asia/Global'
WHERE NOT EXISTS (SELECT 1 FROM airlines WHERE airline_name = 'Cathay Pacific');

-- Step 4: Insert seed flights (flight_number is UNIQUE so INSERT IGNORE skips existing)
INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('EK362', (SELECT id FROM airlines WHERE airline_name = 'Emirates' LIMIT 1), 'DXB', 'SIN', '2026-08-10 22:00:00', '2026-08-11 10:00:00', 180, 300, 450.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('SQ322', (SELECT id FROM airlines WHERE airline_name = 'Singapore Airlines' LIMIT 1), 'SIN', 'LHR', '2026-08-15 23:30:00', '2026-08-16 05:30:00', 150, 280, 780.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('BA117', (SELECT id FROM airlines WHERE airline_name = 'British Airways' LIMIT 1), 'LHR', 'JFK', '2026-08-20 11:00:00', '2026-08-20 14:00:00', 200, 350, 620.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('QF7', (SELECT id FROM airlines WHERE airline_name = 'Qantas' LIMIT 1), 'SYD', 'SIN', '2026-08-12 21:00:00', '2026-08-13 03:30:00', 120, 250, 390.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('CX531', (SELECT id FROM airlines WHERE airline_name = 'Cathay Pacific' LIMIT 1), 'HKG', 'NRT', '2026-08-18 09:00:00', '2026-08-18 13:30:00', 90, 200, 320.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('EK2', (SELECT id FROM airlines WHERE airline_name = 'Emirates' LIMIT 1), 'DXB', 'LHR', '2026-08-14 08:30:00', '2026-08-14 13:00:00', 30, 60, 2400.00, 'BUSINESS', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('SQ501', (SELECT id FROM airlines WHERE airline_name = 'Singapore Airlines' LIMIT 1), 'SIN', 'DXB', '2026-08-22 01:00:00', '2026-08-22 04:30:00', 160, 280, 510.00, 'ECONOMY', 'MANUAL', NOW());

INSERT IGNORE INTO flights (flight_number, airline_id, origin_airport_code, destination_airport_code, departure_date_time, arrival_date_time, available_seats, total_seats, price, cabin_class, source, created_at)
VALUES ('BA308', (SELECT id FROM airlines WHERE airline_name = 'British Airways' LIMIT 1), 'LHR', 'CDG', '2026-08-25 07:00:00', '2026-08-25 09:20:00', 100, 180, 180.00, 'ECONOMY', 'MANUAL', NOW());
