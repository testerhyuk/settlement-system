CREATE DATABASE IF NOT EXISTS settlement_analytics;

CREATE TABLE IF NOT EXISTS settlement_analytics.campaign_budget_metrics
(
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    charged_count UInt64,
    deducted_count UInt64,
    rejected_count UInt64,

    total_charged_amount Decimal(18, 2),
    total_deducted_amount Decimal(18, 2),

    insufficient_budget_count UInt64,
    reject_rate Float64,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (campaign_id, window_start);

CREATE TABLE IF NOT EXISTS settlement_analytics.ad_impression_metrics
(
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    impression_count UInt64,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (campaign_id, window_start);

CREATE TABLE IF NOT EXISTS settlement_analytics.ad_click_metrics
(
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    click_count UInt64,
    total_click_cost Decimal(18,2),

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (campaign_id, window_start);

CREATE TABLE IF NOT EXISTS settlement_analytics.ad_conversion_metrics
(
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    conversion_count UInt64,
    total_conversion_amount Decimal(18, 2),

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (campaign_id, window_start);

CREATE TABLE IF NOT EXISTS settlement_analytics.campaign_performance_metrics
(
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    impression_count UInt64,
    click_count UInt64,
    conversion_count UInt64,

    total_deducted_amount Decimal(18, 2),
    total_conversion_amount Decimal(18, 2),

    ctr Float64,
    cvr Float64,
    roas Decimal(18, 4),

    partial Bool,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (campaign_id, window_start);

CREATE TABLE IF NOT EXISTS settlement_analytics.segment_performance_metrics
(
    segment_id String,
    campaign_id String,
    window_start DateTime64(3, 'UTC'),
    window_end DateTime64(3, 'UTC'),

    impression_count UInt64,
    click_count UInt64,
    conversion_count UInt64,

    total_deducted_amount Decimal(18, 2),
    total_conversion_amount Decimal(18, 2),

    ctr Float64,
    cvr Float64,
    roas Decimal(18, 4),

    partial Bool,

    created_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(window_start)
ORDER BY (segment_id, campaign_id, window_start);
