package com.hyuk.settlement.analytics.segment;

import com.hyuk.settlement.shared.UserSegmentEvent;
import org.apache.flink.streaming.api.datastream.DataStream;

public record UserSegmentPipelineResult(DataStream<UserSegmentEvent> eventStream,
                                        DataStream<String> parseErrorStream) {
}
