package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.analytics.config.AnalyticsProperties;
import com.hyuk.settlement.shared.AdClickEvent;
import com.hyuk.settlement.shared.AdConversionEvent;
import com.hyuk.settlement.shared.AdImpressionEvent;
import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;

public class SegmentPerformancePipeline {
    private final AnalyticsProperties properties;

    public SegmentPerformancePipeline(AnalyticsProperties properties) {
        this.properties = properties;
    }

    public SegmentPerformancePipelineResult build(
            DataStream<AdImpressionEvent> impressionEventStream,
            DataStream<AdClickEvent> clickEventStream,
            DataStream<AdConversionEvent> conversionEventStream,
            DataStream<UserSegmentEvent> userSegmentEventStream
    ) {
        DataStream<SegmentPerformanceEvent> impressionStream =
                impressionEventStream.map(SegmentPerformanceEvent::impression);

        DataStream<SegmentPerformanceEvent> clickStream =
                clickEventStream.map(SegmentPerformanceEvent::click);

        DataStream<SegmentPerformanceEvent> conversionStream =
                conversionEventStream.map(SegmentPerformanceEvent::conversion);

        DataStream<SegmentPerformanceEvent> userSegmentStream =
                userSegmentEventStream.map(SegmentPerformanceEvent::userSegment);

        DataStream<SegmentPerformanceEvent> segmentPerformanceEventStream =
                impressionStream
                        .union(clickStream, conversionStream, userSegmentStream);

        SingleOutputStreamOperator<SegmentPerformanceInput> segmentPerformanceInputStream =
                segmentPerformanceEventStream
                        .keyBy(SegmentPerformanceEvent::userId)
                        .process(new SegmentPerformanceEnrichmentFunction());

        DataStream<UnknownSegmentEvent> unknownSegmentStream =
                segmentPerformanceInputStream.getSideOutput(
                        SegmentPerformanceEnrichmentFunction.UNKNOWN_SEGMENT_TAG
                );

        DataStream<SegmentPerformanceMetric> metricStream = segmentPerformanceInputStream
                .keyBy(input -> input.getSegmentId() + "|" + input.getCampaignId())
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(properties.getFlinkMetricWindowMinutes())))
                .aggregate(
                        new SegmentPerformanceAggregateFunction(),
                        new SegmentPerformanceWindowFunction()
                );

        return new SegmentPerformancePipelineResult(metricStream, unknownSegmentStream);
    }
}
