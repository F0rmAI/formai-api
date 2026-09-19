CREATE TABLE IF NOT EXISTS iam.user_roles (
    user_id UUID NOT NULL REFERENCES iam.users(id),
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Backfill: every account that existed before RBAC gets the baseline role, so nobody
-- is locked out once JwtAuthenticationFilter starts requiring an authority to match.
INSERT INTO iam.user_roles (user_id, role)
SELECT id, 'REGISTERED_USER' FROM iam.users
ON CONFLICT DO NOTHING;
