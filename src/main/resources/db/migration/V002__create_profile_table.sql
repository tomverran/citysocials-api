CREATE TABLE user_profiles (
    user_id     INT         NOT NULL PRIMARY KEY REFERENCES users (id),
    location    VARCHAR     NOT NULL,
    interests   VARCHAR[]   NOT NULL, -- also likely to change
    created_at  TIMESTAMPTZ NOT NULL
);
