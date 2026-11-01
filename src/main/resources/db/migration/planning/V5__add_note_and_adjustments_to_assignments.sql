-- FR-032 / FR-033: the routine stays one template for every client; each assignment carries the
-- trainer's note and the client's own target load and rest per exercise of a session.
ALTER TABLE planning.client_plan_assignments ADD COLUMN IF NOT EXISTS note VARCHAR(500);
ALTER TABLE planning.client_plan_assignments
    ADD COLUMN IF NOT EXISTS adjustments_json TEXT NOT NULL DEFAULT '[]';
