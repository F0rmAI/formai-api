-- FR-033: the note the trainer left with the assignment, shown to the client with the routine.
ALTER TABLE tracking.active_routines ADD COLUMN IF NOT EXISTS trainer_note VARCHAR(500);
