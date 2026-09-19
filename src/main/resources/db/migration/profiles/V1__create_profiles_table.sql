CREATE TABLE IF NOT EXISTS profiles.profiles (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(150),
    last_name VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
