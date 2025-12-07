-- Email logs table
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    message_id VARCHAR(255),
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_logs_recipient_email ON email_logs(recipient_email);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at);
CREATE INDEX idx_email_logs_template_name ON email_logs(template_name);

COMMENT ON TABLE email_logs IS 'Log of all email send attempts';
COMMENT ON COLUMN email_logs.status IS 'SENT, FAILED, or PENDING';
COMMENT ON COLUMN email_logs.message_id IS 'AWS SES message ID';

-- Push notification tokens table
CREATE TABLE push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    endpoint_arn VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_push_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_push_tokens_user_id ON push_tokens(user_id);
CREATE INDEX idx_push_tokens_device_token ON push_tokens(device_token);

COMMENT ON TABLE push_tokens IS 'Mobile device push notification tokens';
COMMENT ON COLUMN push_tokens.platform IS 'ANDROID or IOS';
COMMENT ON COLUMN push_tokens.endpoint_arn IS 'AWS SNS endpoint ARN';

-- Failed notifications table (Dead Letter Queue)
CREATE TABLE failed_notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(20) NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    recipient_address VARCHAR(500) NOT NULL,
    message_content TEXT NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    succeeded_at TIMESTAMP
);

CREATE INDEX idx_failed_notifications_status ON failed_notifications(status);
CREATE INDEX idx_failed_notifications_next_retry ON failed_notifications(next_retry_at);
CREATE INDEX idx_failed_notifications_created_at ON failed_notifications(created_at);

COMMENT ON TABLE failed_notifications IS 'Dead letter queue for failed notifications';
COMMENT ON COLUMN failed_notifications.notification_type IS 'EMAIL, SMS, or PUSH';
COMMENT ON COLUMN failed_notifications.status IS 'PENDING, RETRYING, FAILED, or SUCCEEDED';
