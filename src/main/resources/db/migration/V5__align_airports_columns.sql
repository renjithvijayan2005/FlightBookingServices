-- Align legacy/local airports schema with current JPA model.
-- MySQL-compatible conditional ADD COLUMN via INFORMATION_SCHEMA.

SELECT COUNT(*) INTO @has_city
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'airports'
  AND COLUMN_NAME = 'city';

SET @sql_city = IF(@has_city = 0,
    'ALTER TABLE airports ADD COLUMN city VARCHAR(100) NULL',
    'SELECT 1');
PREPARE _stmt FROM @sql_city;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SELECT COUNT(*) INTO @has_country
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'airports'
  AND COLUMN_NAME = 'country';

SET @sql_country = IF(@has_country = 0,
    'ALTER TABLE airports ADD COLUMN country VARCHAR(100) NULL',
    'SELECT 1');
PREPARE _stmt FROM @sql_country;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SELECT COUNT(*) INTO @has_location
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'airports'
  AND COLUMN_NAME = 'location';

SET @sql_location = IF(@has_location = 0,
    'ALTER TABLE airports ADD COLUMN location VARCHAR(255) NULL',
    'SELECT 1');
PREPARE _stmt FROM @sql_location;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SELECT COUNT(*) INTO @has_facilities
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'airports'
  AND COLUMN_NAME = 'facilities';

SET @sql_facilities = IF(@has_facilities = 0,
    'ALTER TABLE airports ADD COLUMN facilities VARCHAR(500) NULL',
    'SELECT 1');
PREPARE _stmt FROM @sql_facilities;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SELECT COUNT(*) INTO @has_timezone
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'airports'
  AND COLUMN_NAME = 'timezone';

SET @sql_timezone = IF(@has_timezone = 0,
    'ALTER TABLE airports ADD COLUMN timezone VARCHAR(50) NULL',
    'SELECT 1');
PREPARE _stmt FROM @sql_timezone;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

UPDATE airports
SET timezone = COALESCE(timezone, 'UTC');
