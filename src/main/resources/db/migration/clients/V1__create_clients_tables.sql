-- One trainer profile per iam account.
CREATE TABLE IF NOT EXISTS clients.trainers (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    registered_at TIMESTAMP NOT NULL
);

-- A client's id is the id of their iam account. An email appears only once per trainer.
-- The body profile columns stay null until the trainer fills it in.
CREATE TABLE IF NOT EXISTS clients.clients (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL,
    goal VARCHAR(120),
    height_cm INT,
    weight_kg NUMERIC(5, 2),
    restrictions VARCHAR(500),
    registered_at TIMESTAMP NOT NULL,
    UNIQUE (holder_id, email)
);

CREATE INDEX IF NOT EXISTS idx_clients_holder_id ON clients.clients (holder_id);

-- Every body weight change is kept with its date.
CREATE TABLE IF NOT EXISTS clients.client_weight_records (
    client_id UUID NOT NULL REFERENCES clients.clients(id) ON DELETE CASCADE,
    position INT NOT NULL,
    weight_kg NUMERIC(5, 2) NOT NULL,
    recorded_on DATE NOT NULL,
    PRIMARY KEY (client_id, position)
);
