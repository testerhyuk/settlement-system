CREATE DATABASE IF NOT EXISTS settlement_analytics;

CREATE TABLE IF NOT EXISTS settlement_analytics.daily_campaign_performance_mart
(
    report_date Date,

    campaign_id String,

    impression_count UInt64,
    click_count UInt64,
    conversion_count UInt64,

    total_deducted_amount Decimal(18, 2),
    total_conversion_amount Decimal(18, 2),

    ctr Float64,
    cvr Float64,
    roas Decimal(18, 4),

    batch_run_id String,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(report_date)
ORDER BY (report_date, campaign_id, batch_run_id)
TTL toDateTime(created_at) + INTERVAL 30 DAY;

CREATE TABLE IF NOT EXISTS settlement_analytics.daily_segment_performance_mart
(
    report_date Date,

    segment_id String,
    campaign_id String,

    impression_count UInt64,
    click_count UInt64,
    conversion_count UInt64,

    total_deducted_amount Decimal(18, 2),
    total_conversion_amount Decimal(18, 2),

    ctr Float64,
    cvr Float64,
    roas Decimal(18, 4),

    batch_run_id String,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(report_date)
ORDER BY (report_date, segment_id, campaign_id, batch_run_id)
TTL toDateTime(created_at) + INTERVAL 30 DAY;
