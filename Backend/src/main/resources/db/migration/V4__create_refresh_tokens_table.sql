CREATE TABLE refresh_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                family_id CHAR(36) NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6),
                                created_at DATETIME(6) NOT NULL,
                                updated_at DATETIME(6) NOT NULL,

                                CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
                                CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(id),

                                INDEX idx_refresh_token_user_id (user_id),
                                INDEX idx_refresh_token_family_id (family_id)
);