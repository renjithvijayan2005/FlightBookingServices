-- ============================================================
-- V6__replace_airports_singapore_dubai_kerala.sql
-- Replace all airports with only Singapore, Dubai, and Kerala airports
-- ============================================================

-- Remove airports not in Singapore, Dubai, or Kerala only when they are unused.
-- Existing local databases may already have flights referencing older airport codes.
DELETE FROM airports
WHERE airport_code NOT IN ('SIN', 'DXB', 'DWC', 'COK', 'CCJ', 'TRV', 'CNN')
	AND airport_code NOT IN (
			SELECT origin_airport_code FROM flights
			UNION
			SELECT destination_airport_code FROM flights
	);

-- Singapore
INSERT IGNORE INTO airports (airport_code, airport_name, city, country, location, facilities, timezone) VALUES
('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore', 'Changi, Singapore', 'Jewel, Lounges, Gaming, Shopping, Dining', 'Asia/Singapore');

-- Dubai
INSERT IGNORE INTO airports (airport_code, airport_name, city, country, location, facilities, timezone) VALUES
('DXB', 'Dubai International Airport',       'Dubai', 'UAE', 'Dubai, UAE',               'Luxury Stores, Spa, Lounges, Dining', 'Asia/Dubai'),
('DWC', 'Al Maktoum International Airport',  'Dubai', 'UAE', 'Jebel Ali, Dubai, UAE',    'Shopping, Lounges, Dining',           'Asia/Dubai');

-- Kerala
INSERT IGNORE INTO airports (airport_code, airport_name, city, country, location, facilities, timezone) VALUES
('COK', 'Cochin International Airport',        'Kochi',             'India', 'Nedumbassery, Kochi, Kerala, India',          'Lounges, Shopping, Dining, WiFi', 'Asia/Kolkata'),
('CCJ', 'Calicut International Airport',       'Kozhikode',         'India', 'Karipur, Kozhikode, Kerala, India',           'Shopping, Dining, WiFi',          'Asia/Kolkata'),
('TRV', 'Trivandrum International Airport',    'Thiruvananthapuram','India', 'Thiruvananthapuram, Kerala, India',           'Lounges, Shopping, Dining, WiFi', 'Asia/Kolkata'),
('CNN', 'Kannur International Airport',        'Kannur',            'India', 'Mattannur, Kannur, Kerala, India',            'Shopping, Dining, WiFi',          'Asia/Kolkata');
