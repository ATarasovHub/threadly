-- Refresh tokens are stored as SHA-256 hashes: a database leak must not hand an attacker
-- usable tokens. The raw value only ever exists in the client's cookie.
CREATE TABLE refresh_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    -- All tokens descending from one login share a family, so replaying a rotated token can
    -- invalidate every session that grew out of it.
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family_id ON refresh_tokens (family_id);
