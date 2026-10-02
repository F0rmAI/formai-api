-- Each trainer's own exercise catalog: a name appears only once per trainer.
CREATE TABLE IF NOT EXISTS planning.exercises (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(60) NOT NULL,
    equipment VARCHAR(120),
    status VARCHAR(20) NOT NULL,
    UNIQUE (holder_id, name)
);

CREATE TABLE IF NOT EXISTS planning.routines (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_routines_holder_id ON planning.routines (holder_id);

-- Every saved change of a routine is a new row: the history is never overwritten.
CREATE TABLE IF NOT EXISTS planning.routine_versions (
    routine_id UUID NOT NULL REFERENCES planning.routines(id) ON DELETE CASCADE,
    position INT NOT NULL,
    number INT NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    author VARCHAR(64) NOT NULL,
    sessions_json TEXT NOT NULL,
    PRIMARY KEY (routine_id, position)
);

-- One plan per client with every assignment it ever had.
CREATE TABLE IF NOT EXISTS planning.client_plans (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL UNIQUE,
    holder_id VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS planning.client_plan_assignments (
    client_plan_id UUID NOT NULL REFERENCES planning.client_plans(id) ON DELETE CASCADE,
    position INT NOT NULL,
    routine_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    PRIMARY KEY (client_plan_id, position)
);
