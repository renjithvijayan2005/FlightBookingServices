-- ============================================================
-- V2__seed_data.sql — Reference data
-- ============================================================

INSERT IGNORE INTO airports (airport_code, airport_name, city, country, location, facilities, timezone) VALUES
('JFK', 'John F. Kennedy International Airport', 'New York',   'USA',       'Queens, New York, USA',  'Lounges, WiFi, Shopping',   'America/New_York'),
('LHR', 'Heathrow Airport',                       'London',     'UK',        'London, UK',             'Shopping, Dining, Lounges', 'Europe/London'),
('DXB', 'Dubai International Airport',            'Dubai',      'UAE',       'Dubai, UAE',             'Luxury Stores, Spa',        'Asia/Dubai'),
('SIN', 'Singapore Changi Airport',               'Singapore',  'Singapore', 'Changi, Singapore',      'Jewel, Lounges, Gaming',    'Asia/Singapore'),
('SYD', 'Sydney Kingsford Smith Airport',         'Sydney',     'Australia', 'Mascot, NSW, Australia', 'Lounges, Dining',           'Australia/Sydney'),
('CDG', 'Charles de Gaulle Airport',              'Paris',      'France',    'Roissy, France',         'Shopping, Art Galleries',   'Europe/Paris'),
('HKG', 'Hong Kong International Airport',        'Hong Kong',  'China',     'Lantau Island, HK',      'Shopping, Dining',          'Asia/Hong_Kong'),
('NRT', 'Narita International Airport',           'Tokyo',      'Japan',     'Narita, Chiba, Japan',   'Shopping, Lounges',         'Asia/Tokyo');

INSERT IGNORE INTO airlines (airline_code, airline_name, contact_number, operating_region) VALUES
('EK', 'Emirates',           '+97142144444',   'Global'),
('BA', 'British Airways',    '+442087385050',  'Europe/Global'),
('SQ', 'Singapore Airlines', '+6562238888',    'Asia/Global'),
('QF', 'Qantas',             '+1800227467',    'Australia/Global'),
('CX', 'Cathay Pacific',     '+85227473333',   'Asia/Global');
