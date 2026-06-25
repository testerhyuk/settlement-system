package com.hyuk.settlement.analytics.config;

import lombok.Builder;
import lombok.Getter;
import org.apache.flink.api.java.utils.ParameterTool;

@Getter
@Builder
public class AnalyticsProperties {
    private String bootstrapServers;

    private String budgetResultsTopic;
    private String budgetResultsGroup;

    private String adImpressionsTopic;
    private String adImpressionsGroup;

    private String adClicksTopic;
    private String adClicksGroup;

    private String adConversionsTopic;
    private String adConversionsGroup;

    private String userSegmentsTopic;
    private String userSegmentsGroup;

    private String clickHouseUrl;
    private String clickHouseUsername;
    private String clickHousePassword;
    private int clickHouseBatchSize;
    private long clickHouseBatchIntervalMs;
    private int clickHouseMaxRetries;

    private int flinkParallelism;
    private long flinkCheckpointIntervalMs;
    private long flinkCheckpointTimeoutMs;
    private long flinkMinPauseBetweenCheckpointsMs;
    private int flinkRestartAttempts;
    private long flinkRestartDelayMs;
    private String flinkNetworkMemoryMin;
    private String flinkNetworkMemoryMax;

    private long watermarkOutOfOrdernessSeconds;
    private long idleTimeoutSeconds;

    private long performanceEmitTimeoutMs;
    private int flinkTolerableCheckpointFailureNumber;
    private String flinkPrometheusMetricsPort;

    private String budgetResultsParseDlqTopic;
    private String adImpressionsParseDlqTopic;
    private String adClicksParseDlqTopic;
    private String adConversionsParseDlqTopic;
    private String userSegmentsParseDlqTopic;
    private String unknownSegmentsDlqTopic;

    private long flinkMetricWindowMinutes;

    public static AnalyticsProperties from(ParameterTool parameters) {
        return AnalyticsProperties.builder()
                .bootstrapServers(parameters.getRequired("kafka.bootstrap.servers"))
                .budgetResultsTopic(parameters.getRequired("kafka.topic.budget-results"))
                .budgetResultsGroup(parameters.getRequired("kafka.group.budget-results"))
                .adImpressionsTopic(parameters.getRequired("kafka.topic.ad-impressions"))
                .adImpressionsGroup(parameters.getRequired("kafka.group.ad-impressions"))
                .adClicksTopic(parameters.getRequired("kafka.topic.ad-clicks"))
                .adClicksGroup(parameters.getRequired("kafka.group.ad-clicks"))
                .adConversionsTopic(parameters.getRequired("kafka.topic.ad-conversions"))
                .adConversionsGroup(parameters.getRequired("kafka.group.ad-conversions"))
                .userSegmentsTopic(parameters.getRequired("kafka.topic.user-segments"))
                .userSegmentsGroup(parameters.getRequired("kafka.group.user-segments"))
                .clickHouseUrl(parameters.getRequired("clickhouse.url"))
                .clickHouseUsername(parameters.getRequired("clickhouse.username"))
                .clickHousePassword(parameters.getRequired("clickhouse.password"))
                .clickHouseBatchSize(parameters.getInt("clickhouse.batch-size", 500))
                .clickHouseBatchIntervalMs(parameters.getLong("clickhouse.batch-interval-ms", 1000L))
                .clickHouseMaxRetries(parameters.getInt("clickhouse.max-retries", 3))
                .flinkParallelism(parameters.getInt("flink.parallelism", 12))
                .flinkCheckpointIntervalMs(parameters.getLong("flink.checkpoint-interval-ms", 10000L))
                .flinkCheckpointTimeoutMs(parameters.getLong("flink.checkpoint-timeout-ms", 60000L))
                .flinkMinPauseBetweenCheckpointsMs(parameters.getLong("flink.min-pause-between-checkpoints-ms", 5000L))
                .flinkRestartAttempts(parameters.getInt("flink.restart-attempts", 3))
                .flinkRestartDelayMs(parameters.getLong("flink.restart-delay-ms", 5000L))
                .flinkNetworkMemoryMin(parameters.get("flink.network-memory-min", "128mb"))
                .flinkNetworkMemoryMax(parameters.get("flink.network-memory-max", "128mb"))
                .watermarkOutOfOrdernessSeconds(parameters.getLong("flink.watermark-out-of-orderness-seconds", 5L))
                .idleTimeoutSeconds(parameters.getLong("flink.idle-timeout-seconds", 10L))
                .performanceEmitTimeoutMs(parameters.getLong("flink.performance-emit-timeout-ms", 70000L))
                .flinkTolerableCheckpointFailureNumber(
                        parameters.getInt("flink.tolerable-checkpoint-failure-number", 3)
                )
                .flinkPrometheusMetricsPort(parameters.get("flink.prometheus-metrics-port", "9250"))
                .budgetResultsParseDlqTopic(parameters.getRequired("kafka.topic.budget-results-parse-dlq"))
                .adImpressionsParseDlqTopic(parameters.getRequired("kafka.topic.ad-impressions-parse-dlq"))
                .adClicksParseDlqTopic(parameters.getRequired("kafka.topic.ad-clicks-parse-dlq"))
                .adConversionsParseDlqTopic(parameters.getRequired("kafka.topic.ad-conversions-parse-dlq"))
                .userSegmentsParseDlqTopic(parameters.getRequired("kafka.topic.user-segments-parse-dlq"))
                .unknownSegmentsDlqTopic(parameters.getRequired("kafka.topic.unknown-segments-dlq"))
                .flinkMetricWindowMinutes(parameters.getLong("flink.metric-window-minutes", 1))
                .build();
    }
}
