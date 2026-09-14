-- Profile decoration. All optional: an account is usable with none of it filled in.
ALTER TABLE users
    ADD COLUMN bio        VARCHAR(160),
    ADD COLUMN location   VARCHAR(50),
    ADD COLUMN website    VARCHAR(200),
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN banner_url VARCHAR(500);

-- Empty strings and NULL would both mean "not set"; collapsing them to NULL keeps queries simple.
ALTER TABLE users
    ADD CONSTRAINT ck_users_bio_not_empty      CHECK (bio IS NULL OR length(btrim(bio)) > 0),
    ADD CONSTRAINT ck_users_location_not_empty CHECK (location IS NULL OR length(btrim(location)) > 0),
    ADD CONSTRAINT ck_users_website_scheme     CHECK (website IS NULL OR website ~ '^https?://.+');
