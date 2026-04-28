SET search_path TO roomify;

CREATE SEQUENCE IF NOT EXISTS roomify.booking_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE roomify.bookings
(
    id          BIGINT         NOT NULL,
    user_id     BIGINT         NOT NULL,
    place_id    BIGINT         NOT NULL,
    start_date  DATE           NOT NULL,
    end_date    DATE           NOT NULL,
    status      VARCHAR(50)    NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES roomify.users (id),
    CONSTRAINT fk_bookings_place FOREIGN KEY (place_id) REFERENCES roomify.places (id),
    CONSTRAINT chk_booking_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_bookings_user_id ON roomify.bookings (user_id);
CREATE INDEX idx_bookings_place_id ON roomify.bookings (place_id);
CREATE INDEX idx_bookings_status ON roomify.bookings (status);
CREATE INDEX idx_bookings_place_dates ON roomify.bookings (place_id, start_date, end_date);
