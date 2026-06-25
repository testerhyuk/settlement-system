CREATE VIEW IF NOT EXISTS settlement_analytics.daily_campaign_performance_latest AS
SELECT m.*
FROM settlement_analytics.daily_campaign_performance_mart m
INNER JOIN
(
    SELECT
        report_date,
        argMax(batch_run_id, finished_at) AS batch_run_id
    FROM settlement_analytics.daily_mart_batch_runs
    WHERE mart_name = 'daily_campaign_performance_mart'
      AND status = 'SUCCESS'
    GROUP BY report_date
) latest
ON m.report_date = latest.report_date
AND m.batch_run_id = latest.batch_run_id;

CREATE VIEW IF NOT EXISTS settlement_analytics.daily_segment_performance_latest AS
SELECT m.*
FROM settlement_analytics.daily_segment_performance_mart m
INNER JOIN
(
    SELECT
        report_date,
        argMax(batch_run_id, finished_at) AS batch_run_id
    FROM settlement_analytics.daily_mart_batch_runs
    WHERE mart_name = 'daily_segment_performance_mart'
      AND status = 'SUCCESS'
    GROUP BY report_date
) latest
ON m.report_date = latest.report_date
AND m.batch_run_id = latest.batch_run_id;