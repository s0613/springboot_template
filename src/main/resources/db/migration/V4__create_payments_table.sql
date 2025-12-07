-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    payment_key VARCHAR(200) UNIQUE,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    product_name VARCHAR(255) NOT NULL,
    product_description VARCHAR(500),
    customer_email VARCHAR(255),
    customer_name VARCHAR(100),
    customer_phone VARCHAR(20),
    pg_response TEXT,
    failure_reason VARCHAR(500),
    approved_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(500),
    refund_amount DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_payment_key ON payments(payment_key);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

COMMENT ON TABLE payments IS 'Payment transaction records';
COMMENT ON COLUMN payments.status IS 'PENDING, READY, IN_PROGRESS, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED, FAILED';
COMMENT ON COLUMN payments.payment_method IS 'CARD, VIRTUAL_ACCOUNT, EASY_PAY, MOBILE, TRANSFER, etc.';
COMMENT ON COLUMN payments.pg_response IS 'JSON response from payment gateway';
