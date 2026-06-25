package com.hyuk.settlement.analytics.budget;

import org.apache.flink.streaming.api.datastream.DataStream;

public record BudgetMetricPipelineResult(DataStream<CampaignBudgetMetric> metricStream,
                                         DataStream<String> parseErrorStream) {
}