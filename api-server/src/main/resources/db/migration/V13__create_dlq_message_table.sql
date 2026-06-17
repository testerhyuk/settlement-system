CREATE TABLE dlq_message_entity (
    dlq_message_id VARCHAR(255) PRIMARY KEY,
    source_topic VARCHAR(255),
    source_partition INTEGER,
    source_offset BIGINT,
    exception_class VARCHAR(500),
    exception_message TEXT,
    dlq_error_type VARCHAR(50),
    raw_message TEXT,
    dlq_status VARCHAR(50),
    created_at TIMESTAMP
);