-- Authentication and authorization are out of scope, but the API is aware of
-- the concept of a user: every sleep log belongs to one. This table is
-- deliberately minimal - it gives sleep logs a real owner and referential
-- integrity, it does not model accounts.
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY
);

-- There is no sign-up endpoint, so seed a single user. Without it no sleep log
-- could be created at all.
--
-- The id is stated rather than drawn from the sequence, so that running this
-- script again is a no-op instead of seeding a second user. The sequence is
-- then moved past what exists, leaving later inserts free to use it.
INSERT INTO users (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', (SELECT max(id) FROM users));

-- One row per sleep the user logs.
--
-- Times are stored as TIMESTAMPTZ (absolute instants, normalized to UTC).
-- Clients send ISO-8601 values carrying their own offset and render results in
-- the user's local zone; the API neither stores nor assumes a timezone.
--
-- bed_start is the anchor for both the displayed date and the 30-day window: a
-- night is named after the evening it begins, which is how the wireframe reads
-- ("November, 13th" alongside "10:53 pm - 7:05 am").
--
-- Total time in bed is not stored: it is fully derived from the interval, and
-- duplicating it would let the two disagree. Same reasoning for the sleep date.
--
-- Sleeps are intentionally NOT unique per user per day - naps are legitimate
-- entries, and nothing in the requirements limits a user to one log per night.
CREATE TABLE IF NOT EXISTS sleep_log (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id),
    bed_start       TIMESTAMPTZ NOT NULL,
    bed_end         TIMESTAMPTZ NOT NULL,

    -- Stored by name rather than by ordinal: renaming a value costs one UPDATE,
    -- whereas reordering an ordinal enum would silently rewrite history.
    morning_feeling VARCHAR(4)  NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT sleep_log_interval_ordered CHECK (bed_end > bed_start),
    CONSTRAINT sleep_log_feeling_allowed
        CHECK (morning_feeling IN ('BAD', 'OK', 'GOOD'))
);

-- Serves both reads: the latest sleep for a user, and a user's 30-day window.
-- Postgres does not index the referencing side of a foreign key on its own.
CREATE INDEX IF NOT EXISTS sleep_log_user_bed_start_idx ON sleep_log (user_id, bed_start DESC);
