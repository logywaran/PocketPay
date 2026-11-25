
-- ==========================================
-- V1: Initial PocketPay Database Schema
-- ==========================================

-- 1. USERS TABLE
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT chk_users_role
                           CHECK (role IN ('USER', 'ADMIN')),
                       CONSTRAINT chk_users_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;


-- 2. WALLETS TABLE
CREATE TABLE wallets (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                         currency CHAR(3) NOT NULL DEFAULT 'INR',
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT pk_wallets PRIMARY KEY (id),
                         CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
                         CONSTRAINT fk_wallets_user
                             FOREIGN KEY (user_id) REFERENCES users(id),
                         CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
                         CONSTRAINT chk_wallets_status
                             CHECK (status IN ('ACTIVE', 'FROZEN'))
) ENGINE=InnoDB;
