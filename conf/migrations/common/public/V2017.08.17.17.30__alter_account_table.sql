ALTER TABLE account ADD COLUMN terms_approved BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE account SET terms_approved = TRUE;
