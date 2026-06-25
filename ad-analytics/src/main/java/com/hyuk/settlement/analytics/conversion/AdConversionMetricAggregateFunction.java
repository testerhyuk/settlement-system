package com.hyuk.settlement.analytics.conversion;

import com.hyuk.settlement.shared.AdConversionEvent;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.math.BigDecimal;

public class AdConversionMetricAggregateFunction implements AggregateFunction<
        AdConversionEvent,
        AdConversionMetricAccumulator,
        AdConversionMetricAccumulator
        > {
    @Override
    public AdConversionMetricAccumulator createAccumulator() {
        return new AdConversionMetricAccumulator();
    }

    @Override
    public AdConversionMetricAccumulator add(AdConversionEvent event, AdConversionMetricAccumulator accumulator) {
        accumulator.setCampaignId(event.getCampaignId());

        accumulator.setConversionCount(accumulator.getConversionCount() + 1);

        BigDecimal currentAmount = accumulator.getTotalConversionAmount() == null
                ? BigDecimal.ZERO
                : accumulator.getTotalConversionAmount();

        BigDecimal eventAmount = event.getConversionAmount() == null
                ? BigDecimal.ZERO
                : event.getConversionAmount();

        accumulator.setTotalConversionAmount(currentAmount.add(eventAmount));

        return accumulator;
    }

    @Override
    public AdConversionMetricAccumulator getResult(AdConversionMetricAccumulator accumulator) {
        return accumulator;
    }

    @Override
    public AdConversionMetricAccumulator merge(AdConversionMetricAccumulator acc1, AdConversionMetricAccumulator acc2) {
        if (acc1.getCampaignId() == null) {
            acc1.setCampaignId(acc2.getCampaignId());
        }

        BigDecimal amount1 = acc1.getTotalConversionAmount() == null
                ? BigDecimal.ZERO
                : acc1.getTotalConversionAmount();

        BigDecimal amount2 = acc2.getTotalConversionAmount() == null
                ? BigDecimal.ZERO
                : acc2.getTotalConversionAmount();

        acc1.setConversionCount(acc1.getConversionCount() + acc2.getConversionCount());
        acc1.setTotalConversionAmount(amount1.add(amount2));

        return acc1;
    }
}
