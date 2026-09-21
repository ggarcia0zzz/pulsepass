CREATE TABLE venues (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    capacity    INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_venues_code UNIQUE (code),
    CONSTRAINT ck_venues_capacity_positive CHECK (capacity > 0)
);

CREATE INDEX idx_venues_city ON venues (city);


CREATE TABLE artists (
                         id          BIGSERIAL PRIMARY KEY,
                         stage_name  VARCHAR(150) NOT NULL,
                         genre       VARCHAR(100) NOT NULL,
                         country     VARCHAR(100) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         CONSTRAINT uq_artists_stage_name UNIQUE (stage_name)
);


CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       username        VARCHAR(50)  NOT NULL,
                       email           VARCHAR(150) NOT NULL,
                       active          BOOLEAN      NOT NULL DEFAULT TRUE,

                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email)
);


CREATE TABLE user_profiles (
                               id          BIGSERIAL PRIMARY KEY,
                               first_name  VARCHAR(100) NOT NULL,
                               last_name   VARCHAR(100) NOT NULL,
                               city        VARCHAR(100),
                               phone       VARCHAR(30)  NOT NULL,
                               birth_date  DATE         NOT NULL,
                               user_id     BIGINT       NOT NULL,

                               CONSTRAINT fk_user_profiles_user
                                   FOREIGN KEY (user_id) REFERENCES users (id),
                               CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id)
);


CREATE TABLE events (
                        id           BIGSERIAL PRIMARY KEY,
                        event_code   VARCHAR(50)  NOT NULL,
                        name         VARCHAR(200) NOT NULL,
                        category     VARCHAR(30)  NOT NULL,
                        status       VARCHAR(30)  NOT NULL,
                        event_date   TIMESTAMP    NOT NULL,
                        venue_id     BIGINT       NOT NULL,
                        description  VARCHAR(1000),
                        minimum_age  INTEGER NOT NULL DEFAULT 0,

                        CONSTRAINT fk_events_venue
                            FOREIGN KEY (venue_id) REFERENCES venues (id),
                        CONSTRAINT uq_events_event_code UNIQUE (event_code),
                        CONSTRAINT ck_events_category CHECK (category IN ('MUSIC','SPORTS','TECHNOLOGY','EDUCATION','CULTURE','ENTERTAINMENT')),
                        CONSTRAINT ck_events_status CHECK (status IN ('DRAFT','PUBLISHED','SOLD_OUT','CANCELLED','FINISHED'))
);

CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_event_date ON events (event_date);


CREATE TABLE event_artists (
                               event_id   BIGINT NOT NULL,
                               artist_id  BIGINT NOT NULL,

                               PRIMARY KEY (event_id, artist_id),
                               CONSTRAINT fk_event_artists_event
                                   FOREIGN KEY (event_id) REFERENCES events (id),
                               CONSTRAINT fk_event_artists_artist
                                   FOREIGN KEY (artist_id) REFERENCES artists (id)
);

CREATE TABLE tickets (
    id             BIGSERIAL PRIMARY KEY,
    ticket_code    VARCHAR(50)     NOT NULL,
    type           VARCHAR(30)     NOT NULL,
    status         VARCHAR(30)     NOT NULL,
    price          NUMERIC(10, 2)  NOT NULL,
    purchase_date  TIMESTAMP       NOT NULL,
    user_id        BIGINT          NOT NULL,
    event_id       BIGINT          NOT NULL,

    CONSTRAINT fk_tickets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT uq_tickets_ticket_code UNIQUE (ticket_code),
    CONSTRAINT ck_tickets_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_tickets_type CHECK (type IN ('GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT')),
    CONSTRAINT ck_tickets_status CHECK (status IN ('RESERVED', 'PAID', 'CANCELLED', 'USED'))
);

CREATE INDEX idx_tickets_user_id ON tickets (user_id);
CREATE INDEX idx_tickets_event_id ON tickets (event_id);
CREATE INDEX idx_tickets_status ON tickets (status);