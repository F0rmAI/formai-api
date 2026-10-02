-- A client account is created by the trainer without an email: the client chooses it when
-- activating the account from the mobile app. The UNIQUE constraint still holds for the emails set.
ALTER TABLE iam.users
    ALTER COLUMN email DROP NOT NULL;
