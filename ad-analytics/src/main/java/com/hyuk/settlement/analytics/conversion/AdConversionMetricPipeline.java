package com.hyuk.settlement.analytics.conversion;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.AdConversionEvent;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;

public class AdConversionMetricPipeline {
    private final AnalyticsProperties properties;

    public AdConversionMetricPipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public AdConversionMetricPipelineResult build(DataStream<String> conversionStream) {
        SingleOutputStreamOperator<AdConversionEvent> parsedStream = conversionStream
                .process(new AdConversionParseProcessFunction());

        DataStream<String> parseErrorStream = parsedStream.getSideOutput(AdConversionParseProcessFunction.PARSE_ERROR_TAG);

        DataStream<AdConversionEvent> adConversionStream = parsedStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<AdConversionEvent>forBoundedOutOfOrderness(Duration.ofSeconds(properties.getWatermarkOutOfOrdernessSeconds()))
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getOccurredAt().toEpochMilli()
                                )
                                .withIdleness(Duration.ofSeconds(properties.getIdleTimeoutSeconds()))
                );

        DataStream<AdConversionMetric> metricStream = adConversionStream
                .keyBy(AdConversionEvent::getCampaignId)
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(properties.getFlinkMetricWindowMinutes())))
                .aggregate(
                        new AdConversionMetricAggregateFunction(),
                        new AdConversionMetricWindowFunction()
                );

        return new AdConversionMetricPipelineResult(metricStream, adConversionStream, parseErrorStream);
    }
}
