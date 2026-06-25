package com.hyuk.settlement.analytics.click;

import com.hyuk.settlement.shared.AdClickEvent;
import org.apache.flink.streaming.api.datastream.DataStream;

public record AdClickMetricPipelineResult(
        DataStream<AdClickMetric> metricStream,
        DataStream<AdClickEvent> eventStream,
        DataStream<String> parseErrorStream) {
}
