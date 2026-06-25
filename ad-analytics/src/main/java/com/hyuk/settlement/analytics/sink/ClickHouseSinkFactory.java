package com.hyuk.settlement.analytics.sink;

import com.hyuk.settlement.analytics.budget.CampaignBudgetMetric;
import com.hyuk.settlement.analytics.click.AdClickMetric;
import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.analytics.conversion.AdConversionMetric;
import com.hyuk.settlement.analytics.impression.AdImpressionMetric;
import com.hyuk.settlement.analytics.performance.CampaignPerformanceMetric;
import com.hyuk.settlement.analytics.segment.SegmentPerformanceMetric;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.core.datastream.sink.JdbcSink;

public class ClickHouseSinkFactory {
    private static final String CLICKHOUSE_DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";

    private final AnalyticsProperties properties;

    public ClickHouseSinkFactory(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public Sink<CampaignBudgetMetric> campaignBudgetMetricSink() {
        String sql = """
                INSERT INTO settlement_analytics.campaign_budget_metrics
                (
                    campaign_id,
                    window_start,
                    window_end,
                    charged_count,
                    deducted_count,
                    rejected_count,
                    total_charged_amount,
                    total_deducted_amount,
                    insufficient_budget_count,
                    reject_rate
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return JdbcSink.<CampaignBudgetMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getCampaignId());
                    statement.setObject(2, metric.getWindowStart());
                    statement.setObject(3, metric.getWindowEnd());
                    statement.setLong(4, metric.getChargedCount());
                    statement.setLong(5, metric.getDeductedCount());
                    statement.setLong(6, metric.getRejectedCount());
                    statement.setBigDecimal(7, metric.getTotalChargedAmount());
                    statement.setBigDecimal(8, metric.getTotalDeductedAmount());
                    statement.setLong(9, metric.getInsufficientBudgetCount());
                    statement.setDouble(10, metric.getRejectRate());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    public Sink<AdImpressionMetric> adImpressionMetricSink() {
        String sql = """
                INSERT INTO settlement_analytics.ad_impression_metrics
                (
                    campaign_id,
                    window_start,
                    window_end,
                    impression_count
                )
                VALUES (?, ?, ?, ?)
                """;

        return JdbcSink.<AdImpressionMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getCampaignId());
                    statement.setObject(2, metric.getWindowStart());
                    statement.setObject(3, metric.getWindowEnd());
                    statement.setLong(4, metric.getImpressionCount());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    public Sink<AdClickMetric> adClickMetricSink() {
        String sql = """
                INSERT INTO settlement_analytics.ad_click_metrics
                (
                    campaign_id,
                    window_start,
                    window_end,
                    click_count,
                    total_click_cost
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        return JdbcSink.<AdClickMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getCampaignId());
                    statement.setObject(2, metric.getWindowStart());
                    statement.setObject(3, metric.getWindowEnd());
                    statement.setLong(4, metric.getClickCount());
                    statement.setBigDecimal(5, metric.getTotalClickCost());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    public Sink<AdConversionMetric> adConversionMetricSink() {
        String sql = """
                INSERT INTO settlement_analytics.ad_conversion_metrics
                (
                    campaign_id,
                    window_start,
                    window_end,
                    conversion_count,
                    total_conversion_amount
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        return JdbcSink.<AdConversionMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getCampaignId());
                    statement.setObject(2, metric.getWindowStart());
                    statement.setObject(3, metric.getWindowEnd());
                    statement.setLong(4, metric.getConversionCount());
                    statement.setBigDecimal(5, metric.getTotalConversionAmount());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    public Sink<CampaignPerformanceMetric> campaignPerformanceMetricSink() {
        String sql = """
                INSERT INTO settlement_analytics.campaign_performance_metrics
                (
                    campaign_id,
                    window_start,
                    window_end,
                    impression_count,
                    click_count,
                    conversion_count,
                    total_deducted_amount,
                    total_conversion_amount,
                    ctr,
                    cvr,
                    roas,
                    partial
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return JdbcSink.<CampaignPerformanceMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getCampaignId());
                    statement.setObject(2, metric.getWindowStart());
                    statement.setObject(3, metric.getWindowEnd());
                    statement.setLong(4, metric.getImpressionCount());
                    statement.setLong(5, metric.getClickCount());
                    statement.setLong(6, metric.getConversionCount());
                    statement.setBigDecimal(7, metric.getTotalDeductedAmount());
                    statement.setBigDecimal(8, metric.getTotalConversionAmount());
                    statement.setDouble(9, metric.getCtr());
                    statement.setDouble(10, metric.getCvr());
                    statement.setBigDecimal(11, metric.getRoas());
                    statement.setBoolean(12, metric.isPartial());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    public Sink<SegmentPerformanceMetric> segmentPerformanceMetricSink() {
        String sql = """
            INSERT INTO settlement_analytics.segment_performance_metrics
            (
                segment_id,
                campaign_id,
                window_start,
                window_end,
                impression_count,
                click_count,
                conversion_count,
                total_deducted_amount,
                total_conversion_amount,
                ctr,
                cvr,
                roas,
                partial
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        return JdbcSink.<SegmentPerformanceMetric>builder()
                .withQueryStatement(sql, (statement, metric) -> {
                    statement.setString(1, metric.getSegmentId());
                    statement.setString(2, metric.getCampaignId());
                    statement.setObject(3, metric.getWindowStart());
                    statement.setObject(4, metric.getWindowEnd());
                    statement.setLong(5, metric.getImpressionCount());
                    statement.setLong(6, metric.getClickCount());
                    statement.setLong(7, metric.getConversionCount());
                    statement.setBigDecimal(8, metric.getTotalDeductedAmount());
                    statement.setBigDecimal(9, metric.getTotalConversionAmount());
                    statement.setDouble(10, metric.getCtr());
                    statement.setDouble(11, metric.getCvr());
                    statement.setBigDecimal(12, metric.getRoas());
                    statement.setBoolean(13, metric.isPartial());
                })
                .withExecutionOptions(jdbcExecutionOptions())
                .buildAtLeastOnce(jdbcConnectionOptions());
    }

    private JdbcExecutionOptions jdbcExecutionOptions() {
        return JdbcExecutionOptions.builder()
                .withBatchSize(properties.getClickHouseBatchSize())
                .withBatchIntervalMs(properties.getClickHouseBatchIntervalMs())
                .withMaxRetries(properties.getClickHouseMaxRetries())
                .build();
    }

    private JdbcConnectionOptions jdbcConnectionOptions() {
        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(properties.getClickHouseUrl())
                .withDriverName(CLICKHOUSE_DRIVER)
                .withUsername(properties.getClickHouseUsername())
                .withPassword(properties.getClickHousePassword())
                .build();
    }
}