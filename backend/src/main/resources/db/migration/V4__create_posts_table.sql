CREATE TABLE posts (
    id         BIGSERIAL    PRIMARY KEY,
    author_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content    VARCHAR(500) NOT NULL,
    -- Deleting a post must not orphan its replies, so posts are retired rather than removed.
    deleted_at TIMESTAMPTZ,
    edited_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_posts_content_not_blank CHECK (length(btrim(content)) > 0)
);

-- Serves a profile timeline: one author, newest first. The id breaks ties between posts written
-- in the same millisecond, which also makes it a stable cursor.
CREATE INDEX ix_posts_author_created ON posts (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

-- Serves the global timeline and, later, the feed.
CREATE INDEX ix_posts_created ON posts (created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
