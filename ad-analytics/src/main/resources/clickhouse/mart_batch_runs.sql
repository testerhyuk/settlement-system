CREATE TABLE IF NOT EXISTS settlement_analytics.daily_mart_batch_runs
(
    mart_name String,
    report_date Date,
    batch_run_id String,

    status String,

    source_count UInt64,
    mart_count UInt64,

    started_at DateTime64(3, 'UTC'),
    finished_at Nullable(DateTime64(3, 'UTC')),

    error_message Nullable(String),

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(report_date)
ORDER BY (mart_name, report_date, batch_run_id, created_at)
TTL toDateTime(created_at) + INTERVAL 30 DAY;