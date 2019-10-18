CREATE TABLE users (
    id          SERIAL    PRIMARY KEY,
    name        VARCHAR   NOT NULL,
    twitter_id  VARCHAR   NOT NULL, -- for now
    created_at  TIMESTAMP NOT NULL
);
