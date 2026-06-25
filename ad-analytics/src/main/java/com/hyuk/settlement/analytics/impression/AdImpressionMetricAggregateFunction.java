package com.hyuk.settlement.analytics.impression;

import com.hyuk.settlement.shared.AdImpressionEvent;
import org.apache.flink.api.common.functions.AggregateFunction;

public class AdImpressionMetricAggregateFunction implements AggregateFunction<
        AdImpressionEvent,
        AdImpressionMetricAccumulator,
        AdImpressionMetricAccumulator
        > {
    @Override
    public AdImpressionMetricAccumulator createAccumulator() {
        return new  AdImpressionMetricAccumulator();
    }

    @Override
    public AdImpressionMetricAccumulator add(
            AdImpressionEvent event,
            AdImpressionMetricAccumulator accumulator
    ) {
        accumulator.setCampaignId(event.getCampaignId());

        accumulator.setImpressionCount(accumulator.getImpressionCount() + 1);

        return accumulator;
    }

    @Override
    public AdImpressionMetricAccumulator getResult(AdImpressionMetricAccumulator accumulator) {
        return accumulator;
    }

    @Override
    public AdImpressionMetricAccumulator merge(AdImpressionMetricAccumulator a, AdImpressionMetricAccumulator b) {
        if (a.getCampaignId() == null) {
            a.setCampaignId(b.getCampaignId());
        }

        a.setImpressionCount(a.getImpressionCount() + b.getImpressionCount());
        return a;
    }
}
