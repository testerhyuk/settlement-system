package com.hyuk.settlement.analytics.segment;

import org.apache.flink.streaming.api.datastream.DataStream;

public record SegmentPerformancePipelineResult(
        DataStream<SegmentPerformanceMetric> metricStream,
        DataStream<UnknownSegmentEvent> unknownSegmentStream
) {
}