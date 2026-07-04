-- ============================================================
-- V7__align_flights_columns.sql
-- Align older local flights tables with the current entity shape
-- ============================================================

SET @schema_name = DATABASE();

SET @has_total_seats = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'flights'
      AND column_name = 'total_seats'
);
SET @sql = IF(
    @has_total_seats = 0,
    'ALTER TABLE flights ADD COLUMN total_seats INT NOT NULL DEFAULT 0 AFTER available_seats',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_cabin_class = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'flights'
      AND column_name = 'cabin_class'
);
SET @sql = IF(
    @has_cabin_class = 0,
    'ALTER TABLE flights ADD COLUMN cabin_class VARCHAR(20) DEFAULT ''ECONOMY'' AFTER price',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_amadeus_offer_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'flights'
      AND column_name = 'amadeus_offer_id'
);
SET @sql = IF(
    @has_amadeus_offer_id = 0,
    'ALTER TABLE flights ADD COLUMN amadeus_offer_id VARCHAR(500) NULL AFTER cabin_class',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_source = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'flights'
      AND column_name = 'source'
);
SET @sql = IF(
    @has_source = 0,
    'ALTER TABLE flights ADD COLUMN source VARCHAR(20) DEFAULT ''MANUAL'' AFTER amadeus_offer_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE flights
SET source = 'MANUAL'
WHERE source IS NULL OR source = '';

UPDATE flights
SET total_seats = available_seats
WHERE total_seats = 0;