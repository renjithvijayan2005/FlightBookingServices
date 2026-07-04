-- ============================================================
-- V1__init_schema.sql — Full corrected schema
-- ============================================================

-- Users
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    passport_number VARCHAR(50),
    phone        VARCHAR(30),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User roles (kept as element collection)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    roles   VARCHAR(50)  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_roles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Airports
CREATE TABLE IF NOT EXISTS airports (
    airport_code VARCHAR(3)   PRIMARY KEY,
    airport_name VARCHAR(255) NOT NULL,
    city         VARCHAR(100),
    country      VARCHAR(100),
    location     VARCHAR(255),
    facilities   VARCHAR(500),
    timezone     VARCHAR(50)  DEFAULT 'UTC'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Airlines
CREATE TABLE IF NOT EXISTS airlines (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    airline_code     VARCHAR(3)   UNIQUE,
    airline_name     VARCHAR(255) NOT NULL,
    contact_number   VARCHAR(50),
    operating_region VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Flights (local cache of Amadeus results + manual entries)
CREATE TABLE IF NOT EXISTS flights (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number             VARCHAR(20)  NOT NULL UNIQUE,
    airline_id                BIGINT       NOT NULL,
    origin_airport_code       VARCHAR(3)   NOT NULL,
    destination_airport_code  VARCHAR(3)   NOT NULL,
    departure_date_time       DATETIME(6)  NOT NULL,
    arrival_date_time         DATETIME(6)  NOT NULL,
    available_seats           INT          NOT NULL DEFAULT 0,
    total_seats               INT          NOT NULL DEFAULT 0,
    price                     DECIMAL(10,2) NOT NULL,
    cabin_class               VARCHAR(20)  DEFAULT 'ECONOMY',
    amadeus_offer_id          VARCHAR(500),          -- stores Amadeus offer ID for booking
    source                    VARCHAR(20)  DEFAULT 'MANUAL',  -- MANUAL | AMADEUS
    created_at                DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (airline_id)                REFERENCES airlines(id),
    FOREIGN KEY (origin_airport_code)       REFERENCES airports(airport_code),
    FOREIGN KEY (destination_airport_code)  REFERENCES airports(airport_code),
    INDEX idx_flights_route_date (origin_airport_code, destination_airport_code, departure_date_time),
    INDEX idx_flights_departure  (departure_date_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bookings
CREATE TABLE IF NOT EXISTS bookings (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference    VARCHAR(20)  NOT NULL UNIQUE,
    user_id              BIGINT       NOT NULL,
    flight_id            BIGINT,                          -- NULL for Amadeus-only bookings
    amadeus_order_id     VARCHAR(100),                    -- returned by Amadeus booking API
    num_passengers       INT          NOT NULL DEFAULT 1,
    booking_date         DATETIME(6)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING|CONFIRMED|CANCELLED|FAILED
    payment_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING|PAID|FAILED|REFUNDED
    total_amount         DECIMAL(10,2),
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (user_id)    REFERENCES users(id),
    FOREIGN KEY (flight_id)  REFERENCES flights(id),
    INDEX idx_bookings_user   (user_id),
    INDEX idx_bookings_status (status),
    INDEX idx_bookings_ref    (booking_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Passengers per booking
CREATE TABLE IF NOT EXISTS passengers (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id       BIGINT       NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    date_of_birth    DATE,
    passport_number  VARCHAR(50),
    nationality      VARCHAR(50),
    passenger_type   VARCHAR(20)  DEFAULT 'ADULT',   -- ADULT|CHILD|INFANT
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_passengers_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id            BIGINT         NOT NULL UNIQUE,
    payment_method        VARCHAR(50),                  -- CREDIT_CARD|DEBIT_CARD|PAYPAL
    amount                DECIMAL(10,2)  NOT NULL,
    currency              VARCHAR(3)     DEFAULT 'USD',
    transaction_id        VARCHAR(100),                 -- 3rd-party payment processor ref
    transaction_date_time DATETIME(6),
    status                VARCHAR(20)    DEFAULT 'PENDING',  -- PENDING|SUCCESS|FAILED|REFUNDED
    gateway_response      TEXT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification log
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id   BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL,        -- EMAIL|SMS
    recipient    VARCHAR(255) NOT NULL,
    subject      VARCHAR(255),
    status       VARCHAR(20)  DEFAULT 'SENT',  -- SENT|FAILED
    sent_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_notifications_booking (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
