-- Core account table. Profile fields (avatar, bio, ...) arrive in a later migration.
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_users_username_format CHECK (username ~ '^[A-Za-z0-9_]{3,30}$'),
    CONSTRAINT ck_users_email_format    CHECK (position('@' IN email) > 1)
);

-- Handles and emails are compared case-insensitively: @Andrii and @andrii are one account.
CREATE UNIQUE INDEX ux_users_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX ux_users_email_lower    ON users (LOWER(email));
