-- ============================================================
-- V9: Seat selections, luggage options, extended booking fields
-- ============================================================

SET @schema_name = DATABASE();

-- Add contact info + booking management fields to bookings
SET @has_contact_name = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'contact_name'
);
SET @sql = IF(
    @has_contact_name = 0,
    'ALTER TABLE bookings ADD COLUMN contact_name VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_contact_email = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'contact_email'
);
SET @sql = IF(
    @has_contact_email = 0,
    'ALTER TABLE bookings ADD COLUMN contact_email VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_contact_phone = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'contact_phone'
);
SET @sql = IF(
    @has_contact_phone = 0,
    'ALTER TABLE bookings ADD COLUMN contact_phone VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_confirmation_key = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'confirmation_key'
);
SET @sql = IF(
    @has_confirmation_key = 0,
    'ALTER TABLE bookings ADD COLUMN confirmation_key VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_gdpr_consent = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'gdpr_consent'
);
SET @sql = IF(
    @has_gdpr_consent = 0,
    'ALTER TABLE bookings ADD COLUMN gdpr_consent TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_gdpr_consent_timestamp = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'gdpr_consent_timestamp'
);
SET @sql = IF(
    @has_gdpr_consent_timestamp = 0,
    'ALTER TABLE bookings ADD COLUMN gdpr_consent_timestamp DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_luggage_total = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'luggage_total'
);
SET @sql = IF(
    @has_luggage_total = 0,
    'ALTER TABLE bookings ADD COLUMN luggage_total DECIMAL(10,2) NOT NULL DEFAULT 0.00',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_stripe_payment_intent_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND column_name = 'stripe_payment_intent_id'
);
SET @sql = IF(
    @has_stripe_payment_intent_id = 0,
    'ALTER TABLE bookings ADD COLUMN stripe_payment_intent_id VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uq_bookings_confirmation_key = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'bookings'
      AND index_name = 'uq_bookings_confirmation_key'
);
SET @sql = IF(
    @has_uq_bookings_confirmation_key = 0,
    'ALTER TABLE bookings ADD UNIQUE INDEX uq_bookings_confirmation_key (confirmation_key)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Seat selections (which seat each booking occupies on a flight)
CREATE TABLE IF NOT EXISTS seat_selections (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_id    BIGINT      NOT NULL,
    booking_id   BIGINT      NOT NULL,
    seat_number  VARCHAR(5)  NOT NULL,
    cabin_class  VARCHAR(20) DEFAULT 'ECONOMY',
    UNIQUE KEY uq_seat_flight (flight_id, seat_number),
    FOREIGN KEY (flight_id)  REFERENCES flights(id)  ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_seat_flight (flight_id),
    INDEX idx_seat_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Luggage options catalog
CREATE TABLE IF NOT EXISTS luggage_options (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)   NOT NULL UNIQUE,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(500),
    weight_kg   INT,
    dimensions  VARCHAR(100),
    price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    category    VARCHAR(50)   DEFAULT 'CHECKED',
    active      TINYINT(1)    DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Older local schemas can skip V1 when Flyway baselines at version 2.
-- Recreate passengers here so booking_luggage can safely reference it.
CREATE TABLE IF NOT EXISTS passengers (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id       BIGINT       NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    date_of_birth    DATE,
    passport_number  VARCHAR(50),
    nationality      VARCHAR(50),
    passenger_type   VARCHAR(20)  DEFAULT 'ADULT',
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_passengers_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Luggage items selected per booking
CREATE TABLE IF NOT EXISTS booking_luggage (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id        BIGINT        NOT NULL,
    passenger_id      BIGINT        NULL,
    luggage_option_id BIGINT        NOT NULL,
    quantity          INT           NOT NULL DEFAULT 1,
    price_per_unit    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (booking_id)        REFERENCES bookings(id)       ON DELETE CASCADE,
    FOREIGN KEY (passenger_id)      REFERENCES passengers(id)     ON DELETE SET NULL,
    FOREIGN KEY (luggage_option_id) REFERENCES luggage_options(id),
    INDEX idx_bl_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed luggage options
INSERT IGNORE INTO luggage_options (code, name, description, weight_kg, dimensions, price, category) VALUES
('PERSONAL_ITEM',      'Personal Item (Free)',       'Small bag, under seat — free of charge. Max 40×20×25 cm.',               7,  '40×20×25 cm',     0.00,  'CABIN'),
('CABIN_STANDARD',     'Standard Cabin Bag',         'Overhead bin cabin baggage. Max 55×35×25 cm.',                           10, '55×35×25 cm',    15.00,  'CABIN'),
('CHECKED_20KG',       '20 kg Checked Bag',          'One standard checked baggage up to 20 kg.',                              20, '158 cm linear',  30.00,  'CHECKED'),
('CHECKED_30KG',       '30 kg Checked Bag',          'One checked baggage up to 30 kg.',                                       30, '158 cm linear',  45.00,  'CHECKED'),
('CHECKED_32KG',       '32 kg Heavy Bag',            'Maximum allowable checked baggage weight of 32 kg.',                     32, '158 cm linear',  60.00,  'CHECKED'),
('SPORTS_EQUIPMENT',   'Sports Equipment',           'Bicycle, surfboard, golf bag, ski/snowboard set, etc.',                  23, '300 cm linear',  80.00,  'SPECIAL'),
('MUSICAL_INSTRUMENT', 'Musical Instrument',         'Small to medium instrument in hard case (e.g. guitar, violin).',         10, '150 cm linear',  50.00,  'SPECIAL');
