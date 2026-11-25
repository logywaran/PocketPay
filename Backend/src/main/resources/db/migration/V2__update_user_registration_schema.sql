ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(16) NOT NULL AFTER email,
    ADD CONSTRAINT uk_users_phone_number UNIQUE (phone_number);

ALTER TABLE users
DROP CHECK chk_users_status;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'));