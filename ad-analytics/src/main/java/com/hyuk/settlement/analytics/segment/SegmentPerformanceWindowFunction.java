package com.hyuk.settlement.analytics.segment;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class SegmentPerformanceWindowFunction extends ProcessWindowFunction<
        SegmentPerformanceAccumulator,
        SegmentPerformanceMetric,
        String,
        TimeWindow
        > {
    private static final ZoneOffset ZONE_ID = ZoneOffset.UTC;
    private static final int RATE_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public void process(
            String key,
            Context context,
            Iterable<SegmentPerformanceAccumulator> elements,
            Collector<SegmentPerformanceMetric> out
    ) {
        SegmentPerformanceAccumulator accumulator = elements.iterator().next();

        LocalDateTime windowStart = Instant.ofEpochMilli(context.window().getStart())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        LocalDateTime windowEnd = Instant.ofEpochMilli(context.window().getEnd())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        BigDecimal totalClickCost = accumulator.getTotalClickCost() == null
                ? BigDecimal.ZERO
                : accumulator.getTotalClickCost();

        BigDecimal totalConversionAmount = accumulator.getTotalConversionAmount() == null
                ? BigDecimal.ZERO
                : accumulator.getTotalConversionAmount();

        double ctr = 0.0;
        if (accumulator.getImpressionCount() > 0) {
            ctr = (double) accumulator.getClickCount() / accumulator.getImpressionCount();
        }

        double cvr = 0.0;
        if (accumulator.getClickCount() > 0) {
            cvr = (double) accumulator.getConversionCount() / accumulator.getClickCount();
        }

        BigDecimal roas = BigDecimal.ZERO;
        if (totalClickCost.compareTo(BigDecimal.ZERO) > 0) {
            roas = totalConversionAmount.divide(totalClickCost, RATE_SCALE, ROUNDING_MODE);
        }

        out.collect(SegmentPerformanceMetric.builder()
                .segmentId(accumulator.getSegmentId())
                .campaignId(accumulator.getCampaignId())
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .impressionCount(accumulator.getImpressionCount())
                .clickCount(accumulator.getClickCount())
                .conversionCount(accumulator.getConversionCount())
                .totalDeductedAmount(totalClickCost)
                .totalConversionAmount(totalConversionAmount)
                .ctr(ctr)
                .cvr(cvr)
                .roas(roas)
                .partial(false)
                .build());
    }
}
