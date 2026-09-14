-- A reply is a post with a parent. Keeping them in one table means a reply can itself be
-- replied to, liked and reposted without any special casing.
ALTER TABLE posts
    ADD COLUMN parent_id BIGINT REFERENCES posts (id) ON DELETE CASCADE;

-- A thread reads oldest first, unlike every other listing in the API.
CREATE INDEX ix_posts_parent ON posts (parent_id, created_at, id)
    WHERE deleted_at IS NULL;

-- Feeds and profile timelines show root posts only, so they filter on parent_id IS NULL.
-- Replacing the earlier indexes keeps that filter inside the index rather than after it.
DROP INDEX ix_posts_created;
DROP INDEX ix_posts_author_created;

CREATE INDEX ix_posts_root_created ON posts (created_at DESC, id DESC)
    WHERE deleted_at IS NULL AND parent_id IS NULL;

CREATE INDEX ix_posts_author_root_created ON posts (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL AND parent_id IS NULL;
