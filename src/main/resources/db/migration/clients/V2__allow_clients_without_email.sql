-- The trainer registers a client with the name only; the email arrives when the client
-- activates the account from the mobile app.
ALTER TABLE clients.clients
    ALTER COLUMN email DROP NOT NULL;
