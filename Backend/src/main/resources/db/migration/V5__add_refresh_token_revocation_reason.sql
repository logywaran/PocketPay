ALTER TABLE refresh_tokens
    ADD COLUMN revocation_reason VARCHAR(30) NULL;