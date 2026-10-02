-- FR-010/FR-012: sessions are scheduled only on the training days of the assignment. Routines
-- synced before this rule trained every day, which is what the default keeps for them.
ALTER TABLE tracking.active_routines
    ADD COLUMN training_days VARCHAR(70) NOT NULL
        DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY';
