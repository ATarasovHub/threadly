-- The follow graph. A row is an edge: follower_id follows followee_id.
CREATE TABLE follows (
    id          BIGSERIAL   PRIMARY KEY,
    follower_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_follows_no_self_follow CHECK (follower_id <> followee_id)
);

-- Following someone twice is the same state as following them once.
CREATE UNIQUE INDEX ux_follows_pair ON follows (follower_id, followee_id);

-- "Who follows this account", newest first, paginated by the same (created_at, id) cursor
-- the timelines use.
CREATE INDEX ix_follows_followee ON follows (followee_id, created_at DESC, id DESC);

-- "Who does this account follow" — also the driving index for the Following feed.
CREATE INDEX ix_follows_follower ON follows (follower_id, created_at DESC, id DESC);
