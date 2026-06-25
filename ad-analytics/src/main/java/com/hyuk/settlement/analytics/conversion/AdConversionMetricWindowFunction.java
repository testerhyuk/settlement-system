package com.hyuk.settlement.analytics.conversion;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AdConversionMetricWindowFunction extends ProcessWindowFunction<
        AdConversionMetricAccumulator,
        AdConversionMetric,
        String,
        TimeWindow
        > {
    private static final ZoneOffset ZONE_ID = ZoneOffset.UTC;

    @Override
    public void process(String campaignId, ProcessWindowFunction<AdConversionMetricAccumulator, AdConversionMetric, String, TimeWindow>.Context context, Iterable<AdConversionMetricAccumulator> iterable, Collector<AdConversionMetric> collector) throws Exception {
        AdConversionMetricAccumulator accumulator = iterable.iterator().next();

        LocalDateTime windowStart = Instant.ofEpochMilli(context.window().getStart()).atZone(ZONE_ID).toLocalDateTime();
        LocalDateTime windowEnd = Instant.ofEpochMilli(context.window().getEnd()).atZone(ZONE_ID).toLocalDateTime();

        collector.collect(AdConversionMetric.builder()
                .campaignId(campaignId)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .conversionCount(accumulator.getConversionCount())
                .totalConversionAmount(accumulator.getTotalConversionAmount())
                .build()
        );
    }
}
