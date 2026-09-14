CREATE TABLE notifications (
    id           BIGSERIAL   PRIMARY KEY,
    recipient_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(20) NOT NULL,
    -- The post the notification is about: absent for a follow.
    post_id      BIGINT      REFERENCES posts (id) ON DELETE CASCADE,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_notifications_no_self CHECK (recipient_id <> actor_id)
);

-- The inbox, newest first.
CREATE INDEX ix_notifications_recipient ON notifications (recipient_id, created_at DESC, id DESC);

-- The unread badge.
CREATE INDEX ix_notifications_unread ON notifications (recipient_id)
    WHERE read_at IS NULL;
