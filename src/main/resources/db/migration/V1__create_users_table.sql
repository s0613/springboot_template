-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    birth_date DATE,
    gender VARCHAR(10),
    consent_privacy BOOLEAN NOT NULL DEFAULT FALSE,
    consent_service BOOLEAN NOT NULL DEFAULT FALSE,
    consent_marketing BOOLEAN NOT NULL DEFAULT FALSE,
    password VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),
    user_type VARCHAR(20) NOT NULL DEFAULT 'MASTER',
    account_type VARCHAR(20) NOT NULL DEFAULT 'SENIOR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    deletion_reason VARCHAR(50),
    detailed_deletion_reason VARCHAR(500),
    master_user_id BIGINT,
    login_code VARCHAR(6),
    CONSTRAINT fk_users_master_user FOREIGN KEY (master_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id);
CREATE INDEX idx_users_master_user_id ON users(master_user_id);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'User account information';
COMMENT ON COLUMN users.user_type IS 'MASTER or SUB_ACCOUNT';
COMMENT ON COLUMN users.account_type IS 'SENIOR, CAREGIVER, or ADMIN';
COMMENT ON COLUMN users.login_code IS '6-digit code for sub-accounts';
