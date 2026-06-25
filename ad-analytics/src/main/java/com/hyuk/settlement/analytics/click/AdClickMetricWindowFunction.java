package com.hyuk.settlement.analytics.click;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AdClickMetricWindowFunction extends ProcessWindowFunction<
        AdClickMetricAccumulator, AdClickMetric, String, TimeWindow
        > {
    private static final ZoneOffset ZONE_ID = ZoneOffset.UTC;

    @Override
    public void process(String campaignId, Context context, Iterable<AdClickMetricAccumulator> iterable, Collector<AdClickMetric> out) throws Exception {
        AdClickMetricAccumulator accumulator = iterable.iterator().next();

        LocalDateTime windowStart = Instant.ofEpochMilli(context.window().getStart()).atZone(ZONE_ID).toLocalDateTime();
        LocalDateTime windowEnd = Instant.ofEpochMilli(context.window().getEnd()).atZone(ZONE_ID).toLocalDateTime();

        BigDecimal totalClickCost = accumulator.getTotalClickCost() == null
                ? BigDecimal.ZERO
                : accumulator.getTotalClickCost();

        out.collect(AdClickMetric.builder()
                .campaignId(campaignId)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .clickCount(accumulator.getClickCount())
                .totalClickCost(totalClickCost)
                .build()
        );
    }
}
