CREATE SCHEMA IF NOT EXISTS roomify;
SET search_path TO roomify;

CREATE SEQUENCE IF NOT EXISTS roomify.place_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE roomify.places
(
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    normalized_address VARCHAR(255) NOT NULL,
    capacity INTEGER,
    price_per_hour NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id BIGINT,

    CONSTRAINT pk_places PRIMARY KEY (id),
    CONSTRAINT fk_places_user FOREIGN KEY (user_id)
        REFERENCES roomify.users (id),
    CONSTRAINT uc_places_owner_name_address
        UNIQUE (user_id, name, address)
);

CREATE INDEX idx_places_type ON roomify.places(type);
CREATE INDEX idx_places_status ON roomify.places(status);
CREATE INDEX idx_places_user_id ON roomify.places(user_id);