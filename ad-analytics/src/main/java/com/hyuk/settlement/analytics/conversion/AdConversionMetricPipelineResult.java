package com.hyuk.settlement.analytics.conversion;

import com.hyuk.settlement.shared.AdConversionEvent;
import org.apache.flink.streaming.api.datastream.DataStream;

public record AdConversionMetricPipelineResult(
        DataStream<AdConversionMetric> metricStream,
        DataStream<AdConversionEvent> eventStream,
        DataStream<String> parseErrorStream) {
}
