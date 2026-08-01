-- The morning feeling is a closed set of three values, so it is modelled as a
-- type rather than as a string with a constraint: the column can then only ever
-- hold one of them, and the allowed values live in one place instead of being
-- restated in a CHECK.
-- Postgres has no CREATE TYPE IF NOT EXISTS, so the catalog is consulted
-- directly. Checking rather than swallowing the error keeps unrelated failures
-- visible.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'morning_feeling') THEN
        CREATE TYPE morning_feeling AS ENUM ('BAD', 'OK', 'GOOD');
    END IF;
END $$;

-- The type subsumes the constraint.
ALTER TABLE sleep_log DROP CONSTRAINT IF EXISTS sleep_log_feeling_allowed;

ALTER TABLE sleep_log
    ALTER COLUMN morning_feeling TYPE morning_feeling
    USING morning_feeling::morning_feeling;
