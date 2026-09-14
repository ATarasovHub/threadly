CREATE TABLE post_likes (
    id         BIGSERIAL   PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Liking twice is the same state as liking once.
CREATE UNIQUE INDEX ux_post_likes_pair ON post_likes (post_id, user_id);

-- Counting likes per post, and resolving "did the viewer like these posts" for a whole page.
CREATE INDEX ix_post_likes_post ON post_likes (post_id);
CREATE INDEX ix_post_likes_user ON post_likes (user_id, created_at DESC, id DESC);
