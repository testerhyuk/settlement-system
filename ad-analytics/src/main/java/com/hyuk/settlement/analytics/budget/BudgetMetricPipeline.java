package com.hyuk.settlement.analytics.budget;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.BudgetResult;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;

public class BudgetMetricPipeline {
    private final AnalyticsProperties properties;

    public BudgetMetricPipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public BudgetMetricPipelineResult build(DataStream<String> budgetStream) {
        SingleOutputStreamOperator<BudgetResult> parsedStream =
                budgetStream.process(new BudgetResultParseProcessFunction());

        DataStream<String> parseErrorStream =
                parsedStream.getSideOutput(BudgetResultParseProcessFunction.PARSE_ERROR_TAG);

        DataStream<BudgetResult> budgetResultStream = parsedStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<BudgetResult>forBoundedOutOfOrderness(
                                        Duration.ofSeconds(properties.getWatermarkOutOfOrdernessSeconds())
                                )
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getProcessedAt().toEpochMilli()
                                )
                                .withIdleness(Duration.ofSeconds(properties.getIdleTimeoutSeconds()))
                );

        DataStream<CampaignBudgetMetric> metricStream = budgetResultStream
                .keyBy(BudgetResult::getCampaignId)
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(properties.getFlinkMetricWindowMinutes())))
                .aggregate(
                        new CampaignBudgetMetricAggregateFunction(),
                        new CampaignBudgetMetricWindowFunction()
                );

        return new BudgetMetricPipelineResult(metricStream, parseErrorStream);
    }
}
