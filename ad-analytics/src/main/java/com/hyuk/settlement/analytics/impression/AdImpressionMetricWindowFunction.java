package com.hyuk.settlement.analytics.impression;


import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AdImpressionMetricWindowFunction
        extends ProcessWindowFunction<
        AdImpressionMetricAccumulator,
        AdImpressionMetric,
        String,
        TimeWindow
        > {
    private static final ZoneOffset ZONE_ID = ZoneOffset.UTC;

    @Override
    public void process(
            String campaignId,
            Context context,
            Iterable<AdImpressionMetricAccumulator> iterable,
            Collector<AdImpressionMetric> out
    ) throws Exception {
        AdImpressionMetricAccumulator accumulator = iterable.iterator().next();

        LocalDateTime windowStart = Instant.ofEpochMilli(context.window().getStart())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        LocalDateTime windowEnd = Instant.ofEpochMilli(context.window().getEnd())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        out.collect(AdImpressionMetric.builder()
                .campaignId(campaignId)
                .impressionCount(accumulator.getImpressionCount())
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build());
    }
}
