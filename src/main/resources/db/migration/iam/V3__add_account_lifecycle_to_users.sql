ALTER TABLE iam.users
    ALTER COLUMN hashed_password DROP NOT NULL,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN failed_sign_in_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP,
    ADD COLUMN activation_code VARCHAR(16) UNIQUE,
    ADD COLUMN activation_code_expires_at TIMESTAMP,
    ADD COLUMN activation_code_used_at TIMESTAMP,
    ADD COLUMN password_reset_token_hash VARCHAR(64) UNIQUE,
    ADD COLUMN password_reset_token_expires_at TIMESTAMP,
    ADD COLUMN password_reset_token_used_at TIMESTAMP,
    ADD COLUMN data_consent_accepted_at TIMESTAMP;

-- Backfill: accounts created before FormAI roles existed were all self sign-ups, which
-- FormAI now registers as trainers. Without TRAINER they could never sign in, since
-- User.canSignInFrom only lets TRAINER/ADMINISTRATOR in through the web platform.
INSERT INTO iam.user_roles (user_id, role)
SELECT u.id, 'TRAINER' FROM iam.users u
WHERE NOT EXISTS (
    SELECT 1 FROM iam.user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENT'
)
ON CONFLICT DO NOTHING;
