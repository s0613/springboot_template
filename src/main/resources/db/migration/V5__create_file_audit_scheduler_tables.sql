-- =====================================================
-- V5: Create File, Audit, and Scheduler tables
-- =====================================================

-- File metadata table
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_url VARCHAR(2000),
    content_type VARCHAR(100),
    file_size BIGINT,
    storage_type VARCHAR(20) NOT NULL DEFAULT 'S3',
    file_category VARCHAR(50),
    uploader_id BIGINT,
    reference_type VARCHAR(100),
    reference_id BIGINT,
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_file_metadata_uploader ON file_metadata(uploader_id);
CREATE INDEX idx_file_metadata_reference ON file_metadata(reference_type, reference_id);
CREATE INDEX idx_file_metadata_category ON file_metadata(file_category);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at);

-- Audit logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(20) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    actor_id BIGINT,
    actor_type VARCHAR(50),
    actor_ip VARCHAR(50),
    actor_user_agent VARCHAR(500),
    old_value TEXT,
    new_value TEXT,
    changed_fields VARCHAR(1000),
    description VARCHAR(500),
    request_path VARCHAR(500),
    request_method VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Scheduler job history table
CREATE TABLE scheduler_job_history (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(200) NOT NULL,
    job_group VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    instance_id VARCHAR(100),
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    result_message VARCHAR(1000),
    error_message TEXT,
    items_processed INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_history_name ON scheduler_job_history(job_name);
CREATE INDEX idx_job_history_status ON scheduler_job_history(status);
CREATE INDEX idx_job_history_started_at ON scheduler_job_history(started_at);

-- Add comments
COMMENT ON TABLE file_metadata IS 'File upload metadata and storage information';
COMMENT ON TABLE audit_logs IS 'Audit trail for all entity changes and user actions';
COMMENT ON TABLE scheduler_job_history IS 'History of scheduled job executions';
