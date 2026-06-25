package com.hyuk.settlement.analytics.impression;

import com.hyuk.settlement.shared.AdImpressionEvent;
import org.apache.flink.streaming.api.datastream.DataStream;

public record AdImpressionsMetricPipelineResult(
        DataStream<AdImpressionMetric> metricStream,
        DataStream<AdImpressionEvent> eventStream,
        DataStream<String> parseErrorStream
    ) {
}
