package com.hyuk.settlement.analytics.click;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.AdClickEvent;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;

public class AdClickMetricPipeline {
    private final AnalyticsProperties properties;

    public AdClickMetricPipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public AdClickMetricPipelineResult build(DataStream<String> clickStream) {
        SingleOutputStreamOperator<AdClickEvent> parsedStream = clickStream.process(new AdClickParseProcessFunction());

        DataStream<String> parseErrorStream = parsedStream.getSideOutput(AdClickParseProcessFunction.PARSE_ERROR_TAG);

        DataStream<AdClickEvent> adClickStream = parsedStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<AdClickEvent>forBoundedOutOfOrderness(Duration.ofSeconds(properties.getWatermarkOutOfOrdernessSeconds()))
                                .withTimestampAssigner(
                                        (event, timestamp) ->
                                                event.getOccurredAt().toEpochMilli()
                                )
                                .withIdleness(Duration.ofSeconds(properties.getIdleTimeoutSeconds()))
                );

        DataStream<AdClickMetric> metricStream = adClickStream
                .keyBy(AdClickEvent::getCampaignId)
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(properties.getFlinkMetricWindowMinutes())))
                .aggregate(
                        new AdClickMetricAggregateFunction(),
                        new AdClickMetricWindowFunction()
                );

        return new AdClickMetricPipelineResult(metricStream, adClickStream, parseErrorStream);
    }
}
