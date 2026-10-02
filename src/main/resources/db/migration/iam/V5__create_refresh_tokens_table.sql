-- NFR-007: refresh tokens renew the access JWT; only their SHA-256 hash is stored.
CREATE TABLE IF NOT EXISTS iam.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES iam.users (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON iam.refresh_tokens (user_id);
