-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for session management';

-- Login attempts table (for rate limiting and security)
CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    attempt_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(100)
);

CREATE INDEX idx_login_attempts_phone_time ON login_attempts(phone_number, attempt_time);
CREATE INDEX idx_login_attempts_ip_time ON login_attempts(ip_address, attempt_time);

COMMENT ON TABLE login_attempts IS 'Track login attempts for rate limiting and security';

-- SMS verification codes table
CREATE TABLE sms_verifications (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    purpose VARCHAR(20) NOT NULL DEFAULT 'SIGNUP',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sms_verifications_phone ON sms_verifications(phone_number);
CREATE INDEX idx_sms_verifications_expires ON sms_verifications(expires_at);

COMMENT ON TABLE sms_verifications IS 'SMS verification codes for signup and password reset';
COMMENT ON COLUMN sms_verifications.purpose IS 'SIGNUP, PASSWORD_RESET, or PHONE_CHANGE';
