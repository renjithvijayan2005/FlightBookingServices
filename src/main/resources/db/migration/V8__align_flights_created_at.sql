-- ============================================================
-- V8__align_flights_created_at.sql
-- Add missing created_at column for older local flights tables
-- ============================================================

SET @schema_name = DATABASE();

SET @has_created_at = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'flights'
      AND column_name = 'created_at'
);
SET @sql = IF(
    @has_created_at = 0,
    'ALTER TABLE flights ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER source',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;