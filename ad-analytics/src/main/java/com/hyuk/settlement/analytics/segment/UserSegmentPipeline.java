package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

import java.time.Duration;

public class UserSegmentPipeline {
    private final AnalyticsProperties properties;

    public UserSegmentPipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public UserSegmentPipelineResult build(DataStream<String> userSegmentStream) {
        SingleOutputStreamOperator<UserSegmentEvent> parsedStream = userSegmentStream.process(new UserSegmentParseProcessFunction());

        DataStream<String> parseErrorStream = parsedStream.getSideOutput(UserSegmentParseProcessFunction.PARSE_ERROR_TAG);

        DataStream<UserSegmentEvent> segmentStream = parsedStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<UserSegmentEvent>forBoundedOutOfOrderness(Duration.ofSeconds(properties.getWatermarkOutOfOrdernessSeconds()))
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getUpdatedAt().toEpochMilli()
                                )
                                .withIdleness(Duration.ofSeconds(properties.getIdleTimeoutSeconds()))
                );

        return new UserSegmentPipelineResult(segmentStream, parseErrorStream);
    }
}
