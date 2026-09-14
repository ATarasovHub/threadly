-- A repost is a post that points at another one. With content it is a quote post; without, a
-- plain repost. Same table again, so reposts are likeable and repliable like anything else.
ALTER TABLE posts
    ADD COLUMN repost_of_id BIGINT REFERENCES posts (id) ON DELETE CASCADE;

-- A plain repost carries no text of its own.
ALTER TABLE posts ALTER COLUMN content DROP NOT NULL;
ALTER TABLE posts DROP CONSTRAINT ck_posts_content_not_blank;
ALTER TABLE posts ADD CONSTRAINT ck_posts_content CHECK (
    (content IS NULL AND repost_of_id IS NOT NULL)
    OR (content IS NOT NULL AND length(btrim(content)) > 0)
);

-- Reposting the same post twice is the same state as once. Quote posts are exempt: quoting one
-- post several times with different commentary is legitimate.
CREATE UNIQUE INDEX ux_posts_plain_repost ON posts (author_id, repost_of_id)
    WHERE content IS NULL AND deleted_at IS NULL;

CREATE INDEX ix_posts_repost_of ON posts (repost_of_id) WHERE deleted_at IS NULL;
