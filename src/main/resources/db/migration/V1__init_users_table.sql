CREATE SEQUENCE IF NOT EXISTS roomify.role_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS roomify.user_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS roomify.email_verification_tokens_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE roomify.roles
(
    id   BIGINT NOT NULL,
    name VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE roomify.user_roles
(
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE roomify.users
(
    id             BIGINT       NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL,
    email_verified BOOLEAN      NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE roomify.email_verification_tokens
(
    id          BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL,
    user_id     BIGINT       NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id)
        REFERENCES roomify.users(id),
    CONSTRAINT uc_evt_token UNIQUE (token)
);

ALTER TABLE roomify.roles
    ADD CONSTRAINT uc_roles_name UNIQUE (name);

ALTER TABLE roomify.users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE roomify.user_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roomify.roles (id);

ALTER TABLE roomify.user_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES roomify.users (id);