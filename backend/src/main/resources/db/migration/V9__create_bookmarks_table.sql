-- Bookmarks are private: only the account that saved a post ever sees that it did.
CREATE TABLE bookmarks (
    id         BIGSERIAL   PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_bookmarks_pair ON bookmarks (post_id, user_id);

-- The saved list, newest first.
CREATE INDEX ix_bookmarks_user ON bookmarks (user_id, created_at DESC, id DESC);
