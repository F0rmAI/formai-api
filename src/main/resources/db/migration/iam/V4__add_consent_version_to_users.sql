ALTER TABLE iam.users
    ADD COLUMN data_consent_version VARCHAR(20);

-- NFR-021: accounts activated before the version was recorded keep their acceptance date,
-- marked 'legacy' because the text they accepted is unknown.
UPDATE iam.users
SET data_consent_version = 'legacy'
WHERE data_consent_accepted_at IS NOT NULL;
