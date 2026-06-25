package com.hyuk.settlement.analytics.click;

import com.hyuk.settlement.shared.AdClickEvent;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.math.BigDecimal;

public class AdClickMetricAggregateFunction implements AggregateFunction<
        AdClickEvent,
        AdClickMetricAccumulator,
        AdClickMetricAccumulator
        > {
    @Override
    public AdClickMetricAccumulator createAccumulator() {
        return new AdClickMetricAccumulator();
    }

    @Override
    public AdClickMetricAccumulator add(AdClickEvent event, AdClickMetricAccumulator accumulator) {
        accumulator.setCampaignId(event.getCampaignId());
        accumulator.setClickCount(accumulator.getClickCount() + 1);

        if (event.getCpcAmount() != null) {
            accumulator.setTotalClickCost(
                    accumulator.getTotalClickCost().add(event.getCpcAmount())
            );
        }

        return accumulator;
    }

    @Override
    public AdClickMetricAccumulator getResult(AdClickMetricAccumulator accumulator) {
        return accumulator;
    }

    @Override
    public AdClickMetricAccumulator merge(AdClickMetricAccumulator a, AdClickMetricAccumulator b) {
        if (a.getCampaignId() == null) {
            a.setCampaignId(b.getCampaignId());
        }

        a.setClickCount(a.getClickCount() + b.getClickCount());
        a.setTotalClickCost(a.getTotalClickCost().add(b.getTotalClickCost()));

        return a;
    }
}
