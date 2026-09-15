-- External sign-in identities. A separate table rather than a google_id column on users, so a
-- second provider does not mean a second column and a third migration.
CREATE TABLE user_identities (
    id               BIGSERIAL   PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(20) NOT NULL,
    -- The provider's own stable subject id, not the email: an email can be reassigned, and
    -- Google's own guidance is to key on `sub`.
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(254),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One provider account maps to at most one Threadly account.
CREATE UNIQUE INDEX ux_user_identities_provider_subject
    ON user_identities (provider, provider_user_id);

-- And one Threadly account links a given provider at most once.
CREATE UNIQUE INDEX ux_user_identities_user_provider
    ON user_identities (user_id, provider);

-- An account created through a provider has no password of its own.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- The real invariant is "every account has a password or at least one linked identity", which
-- spans two tables and so cannot be a CHECK constraint. A trigger could enforce it, at the cost
-- of putting business logic in the database; it is enforced in the service layer instead, and an
-- account with neither is unreachable because both sign-in paths create one or the other.
