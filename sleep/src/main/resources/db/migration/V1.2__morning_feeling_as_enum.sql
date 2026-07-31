-- The morning feeling is a closed set of three values, so it is modelled as a
-- type rather than as a string with a constraint: the column can then only ever
-- hold one of them, and the allowed values live in one place instead of being
-- restated in a CHECK.
CREATE TYPE morning_feeling AS ENUM ('BAD', 'OK', 'GOOD');

-- The type subsumes the constraint.
ALTER TABLE sleep_log DROP CONSTRAINT sleep_log_feeling_allowed;

ALTER TABLE sleep_log
    ALTER COLUMN morning_feeling TYPE morning_feeling
    USING morning_feeling::morning_feeling;
