SET search_path TO roomify;

-- Sample tenant user for demo bookings
INSERT INTO roomify.users (id, first_name, last_name, email, password, enabled, email_verified)
VALUES (nextval('roomify.user_seq'), 'Alice', 'Martin', 'alice.martin@roomify.com',
        '$2a$10$demohashedpasswordxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
        true, true)
ON CONFLICT (email) DO NOTHING;

-- ──────────────────────────────────────────────────────────
-- Booking 1 : PENDING (starts in 5 days)
-- ──────────────────────────────────────────────────────────
WITH tenant AS (SELECT id FROM roomify.users WHERE email = 'alice.martin@roomify.com'),
     place   AS (SELECT id FROM roomify.places  WHERE address = '10 rue de Paris, 75001 Paris' LIMIT 1)
INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes)
SELECT nextval('roomify.booking_seq'),
       tenant.id,
       place.id,
       CURRENT_DATE + 5,
       CURRENT_DATE + 8,
       'PENDING',
       ROUND((SELECT price_per_hour FROM roomify.places WHERE id = place.id) * 3, 2),
       'Réunion d''équipe trimestrielle'
FROM tenant, place;

INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id)
SELECT nextval('roomify.place_unavailability_seq'),
       b.place_id,
       b.start_date,
       b.end_date,
       'BOOKING',
       b.id
FROM roomify.bookings b
WHERE b.status = 'PENDING'
  AND b.start_date = CURRENT_DATE + 5;

-- ──────────────────────────────────────────────────────────
-- Booking 2 : CONFIRMED (starts in 12 days)
-- ──────────────────────────────────────────────────────────
WITH tenant AS (SELECT id FROM roomify.users WHERE email = 'alice.martin@roomify.com'),
     place   AS (SELECT id FROM roomify.places  WHERE address = '5 avenue de Lyon, 69000 Lyon' LIMIT 1)
INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price)
SELECT nextval('roomify.booking_seq'),
       tenant.id,
       place.id,
       CURRENT_DATE + 12,
       CURRENT_DATE + 15,
       'CONFIRMED',
       ROUND((SELECT price_per_hour FROM roomify.places WHERE id = place.id) * 3, 2)
FROM tenant, place;

INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason, booking_id)
SELECT nextval('roomify.place_unavailability_seq'),
       b.place_id,
       b.start_date,
       b.end_date,
       'BOOKING',
       b.id
FROM roomify.bookings b
WHERE b.status = 'CONFIRMED'
  AND b.start_date = CURRENT_DATE + 12;

-- ──────────────────────────────────────────────────────────
-- Booking 3 : COMPLETED (ended 5 days ago)
-- ──────────────────────────────────────────────────────────
WITH tenant AS (SELECT id FROM roomify.users WHERE email = 'alice.martin@roomify.com'),
     place   AS (SELECT id FROM roomify.places  WHERE address = '12 rue de la Photo, 75002 Paris' LIMIT 1)
INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price)
SELECT nextval('roomify.booking_seq'),
       tenant.id,
       place.id,
       CURRENT_DATE - 10,
       CURRENT_DATE - 5,
       'COMPLETED',
       ROUND((SELECT price_per_hour FROM roomify.places WHERE id = place.id) * 5, 2)
FROM tenant, place;

-- ──────────────────────────────────────────────────────────
-- Booking 4 : CANCELLED
-- ──────────────────────────────────────────────────────────
WITH tenant AS (SELECT id FROM roomify.users WHERE email = 'alice.martin@roomify.com'),
     place   AS (SELECT id FROM roomify.places  WHERE address = '10 rue de Paris, 75001 Paris' LIMIT 1)
INSERT INTO roomify.bookings (id, user_id, place_id, start_date, end_date, status, total_price, notes)
SELECT nextval('roomify.booking_seq'),
       tenant.id,
       place.id,
       CURRENT_DATE + 20,
       CURRENT_DATE + 22,
       'CANCELLED',
       ROUND((SELECT price_per_hour FROM roomify.places WHERE id = place.id) * 2, 2),
       'Annulé suite à un changement de planning'
FROM tenant, place;

-- ──────────────────────────────────────────────────────────
-- Owner-blocked period (no booking)
-- ──────────────────────────────────────────────────────────
INSERT INTO roomify.place_unavailability (id, place_id, start_date, end_date, reason)
SELECT nextval('roomify.place_unavailability_seq'),
       p.id,
       CURRENT_DATE + 30,
       CURRENT_DATE + 35,
       'OWNER_BLOCKED'
FROM roomify.places p
WHERE p.address = '10 rue de Paris, 75001 Paris'
LIMIT 1;
