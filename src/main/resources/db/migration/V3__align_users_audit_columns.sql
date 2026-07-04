-- Align legacy/local users schema with current JPA model after baseline.
-- MySQL-compatible conditional ADD COLUMN via INFORMATION_SCHEMA (no IF NOT EXISTS).

SELECT COUNT(*) INTO @has_created_at
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'users'
  AND COLUMN_NAME  = 'created_at';

SET @sql_created = IF(@has_created_at = 0,
    'ALTER TABLE users ADD COLUMN created_at DATETIME(6) NULL',
    'SELECT 1');

PREPARE _stmt FROM @sql_created;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

SELECT COUNT(*) INTO @has_updated_at
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'users'
  AND COLUMN_NAME  = 'updated_at';

SET @sql_updated = IF(@has_updated_at = 0,
    'ALTER TABLE users ADD COLUMN updated_at DATETIME(6) NULL',
    'SELECT 1');

PREPARE _stmt FROM @sql_updated;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

UPDATE users
SET created_at = COALESCE(created_at, NOW(6)),
    updated_at = COALESCE(updated_at, NOW(6));
