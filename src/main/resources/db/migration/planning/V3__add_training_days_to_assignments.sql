-- FR-010: a routine is assigned with the days of the week the client trains. Assignments made
-- before this rule trained every day, which is what the default keeps for them.
ALTER TABLE planning.client_plan_assignments
    ADD COLUMN training_days VARCHAR(70) NOT NULL
        DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY';
