CREATE TABLE IF NOT EXISTS tracking.active_routines (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL UNIQUE,
    routine_id UUID NOT NULL,
    routine_name VARCHAR(120) NOT NULL,
    version INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    days_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_active_routines_routine_id ON tracking.active_routines (routine_id);

-- One session per client and date: the daily job can run again without duplicating it.
CREATE TABLE IF NOT EXISTS tracking.workout_sessions (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    routine_id UUID NOT NULL,
    routine_version INT NOT NULL,
    day_order INT NOT NULL,
    day_label VARCHAR(120) NOT NULL,
    scheduled_for DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    finished_at TIMESTAMP,
    UNIQUE (client_id, scheduled_for)
);

CREATE INDEX IF NOT EXISTS idx_workout_sessions_status_scheduled_for
    ON tracking.workout_sessions (status, scheduled_for);

CREATE TABLE IF NOT EXISTS tracking.workout_session_exercises (
    workout_session_id UUID NOT NULL REFERENCES tracking.workout_sessions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    exercise_id UUID NOT NULL,
    exercise_name VARCHAR(120) NOT NULL,
    sets INT NOT NULL,
    reps INT NOT NULL,
    target_load_kg NUMERIC(6, 2) NOT NULL,
    rest_seconds INT NOT NULL,
    PRIMARY KEY (workout_session_id, position)
);

CREATE TABLE IF NOT EXISTS tracking.workout_session_sets (
    workout_session_id UUID NOT NULL REFERENCES tracking.workout_sessions(id) ON DELETE CASCADE,
    position INT NOT NULL,
    exercise_id UUID NOT NULL,
    set_number INT NOT NULL,
    load_kg NUMERIC(6, 2) NOT NULL,
    reps INT NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    PRIMARY KEY (workout_session_id, position)
);
