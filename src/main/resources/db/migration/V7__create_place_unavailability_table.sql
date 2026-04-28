SET search_path TO roomify;

CREATE SEQUENCE IF NOT EXISTS roomify.place_unavailability_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE roomify.place_unavailability
(
    id         BIGINT      NOT NULL,
    place_id   BIGINT      NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    reason     VARCHAR(50) NOT NULL,
    booking_id BIGINT,

    CONSTRAINT pk_place_unavailability PRIMARY KEY (id),
    CONSTRAINT fk_unavailability_place FOREIGN KEY (place_id) REFERENCES roomify.places (id),
    CONSTRAINT fk_unavailability_booking FOREIGN KEY (booking_id) REFERENCES roomify.bookings (id),
    CONSTRAINT chk_unavailability_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_unavailability_place_dates ON roomify.place_unavailability (place_id, start_date, end_date);
