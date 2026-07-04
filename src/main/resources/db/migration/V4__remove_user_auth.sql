-- Drop FK from bookings to users, make user_id nullable for anonymous bookings
SELECT CONSTRAINT_NAME INTO @fk_name
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'bookings'
  AND COLUMN_NAME = 'user_id'
  AND REFERENCED_TABLE_NAME = 'users'
LIMIT 1;

SET @drop_fk = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE bookings DROP FOREIGN KEY `', @fk_name, '`'),
    'SELECT 1');

PREPARE _stmt FROM @drop_fk;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

ALTER TABLE bookings MODIFY COLUMN user_id BIGINT NULL;
