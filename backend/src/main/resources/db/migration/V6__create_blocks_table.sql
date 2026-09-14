-- Blocking is directed: blocker_id blocked blocked_id. Its effects, however, are mutual —
-- neither side sees the other's posts or profile.
CREATE TABLE blocks (
    id         BIGSERIAL   PRIMARY KEY,
    blocker_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_blocks_no_self_block CHECK (blocker_id <> blocked_id)
);

CREATE UNIQUE INDEX ux_blocks_pair ON blocks (blocker_id, blocked_id);

-- Feeds ask "is there a block in either direction between these two accounts", so both
-- directions need an index.
CREATE INDEX ix_blocks_blocked ON blocks (blocked_id);
