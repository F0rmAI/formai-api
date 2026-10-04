-- A calendar day starts at midnight in each client's own time zone (IANA id reported by the
-- mobile app). Routines synced before this rule keep the zone FormAI started with until the
-- client's device reports its own. last_daily_run_on lets the daily job, which now runs every
-- few minutes, process each client's day exactly once.
ALTER TABLE tracking.active_routines
    ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'America/Lima',
    ADD COLUMN last_daily_run_on DATE;
