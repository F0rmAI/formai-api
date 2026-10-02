-- An exercise may be linked to a published machine of the catalog (US-030).
ALTER TABLE planning.exercises ADD COLUMN IF NOT EXISTS machine_id UUID;
