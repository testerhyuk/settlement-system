package com.hyuk.settlement.analytics.impression;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.AdImpressionEvent;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;

public class AdImpressionMetricPipeline {
    private final AnalyticsProperties properties;

    public AdImpressionMetricPipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public AdImpressionsMetricPipelineResult build(DataStream<String> impressionStream) {
        SingleOutputStreamOperator<AdImpressionEvent> parsedStream =
                impressionStream.process(new AdImpressionParseProcessFunction());

        DataStream<String> parseErrorStream =
                parsedStream.getSideOutput(AdImpressionParseProcessFunction.PARSE_ERROR_TAG);

        DataStream<AdImpressionEvent> adImpressionStream = parsedStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<AdImpressionEvent>forBoundedOutOfOrderness(Duration.ofSeconds(properties.getWatermarkOutOfOrdernessSeconds()))
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getOccurredAt().toEpochMilli()
                                ).withIdleness(Duration.ofSeconds(properties.getIdleTimeoutSeconds()))
                );

        DataStream<AdImpressionMetric> metricStream = adImpressionStream
                .keyBy(AdImpressionEvent::getCampaignId)
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(properties.getFlinkMetricWindowMinutes())))
                .aggregate(
                        new AdImpressionMetricAggregateFunction(),
                        new AdImpressionMetricWindowFunction()
                );

        return new AdImpressionsMetricPipelineResult(metricStream, adImpressionStream, parseErrorStream);
    }
}
