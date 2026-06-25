package com.hyuk.settlement.analytics.budget;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class CampaignBudgetMetricWindowFunction
        extends ProcessWindowFunction<
                CampaignBudgetMetricAccumulator,
                CampaignBudgetMetric,
                String,
                TimeWindow
                > {

    private static final ZoneOffset ZONE_ID = ZoneOffset.UTC;

    @Override
    public void process(
            String campaignId,
            Context context,
            Iterable<CampaignBudgetMetricAccumulator> elements,
            Collector<CampaignBudgetMetric> out
    ) {
        CampaignBudgetMetricAccumulator accumulator = elements.iterator().next();

        long totalCount = accumulator.getChargedCount()
                + accumulator.getDeductedCount()
                + accumulator.getRejectedCount();

        double rejectRate = totalCount == 0
                ? 0.0
                : (double) accumulator.getRejectedCount() / totalCount;

        LocalDateTime windowStart = Instant.ofEpochMilli(context.window().getStart())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        LocalDateTime windowEnd = Instant.ofEpochMilli(context.window().getEnd())
                .atZone(ZONE_ID)
                .toLocalDateTime();

        out.collect(CampaignBudgetMetric.builder()
                .campaignId(campaignId)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .chargedCount(accumulator.getChargedCount())
                .deductedCount(accumulator.getDeductedCount())
                .rejectedCount(accumulator.getRejectedCount())
                .totalChargedAmount(accumulator.getTotalChargedAmount())
                .totalDeductedAmount(accumulator.getTotalDeductedAmount())
                .insufficientBudgetCount(accumulator.getInsufficientBudgetCount())
                .rejectRate(rejectRate)
                .build());
    }
}
