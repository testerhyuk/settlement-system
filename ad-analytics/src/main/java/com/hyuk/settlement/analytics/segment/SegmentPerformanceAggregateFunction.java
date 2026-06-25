package com.hyuk.settlement.analytics.segment;

import org.apache.flink.api.common.functions.AggregateFunction;

import java.math.BigDecimal;

public class SegmentPerformanceAggregateFunction implements AggregateFunction<
        SegmentPerformanceInput,
        SegmentPerformanceAccumulator,
        SegmentPerformanceAccumulator
        > {

    @Override
    public SegmentPerformanceAccumulator createAccumulator() {
        return new SegmentPerformanceAccumulator();
    }

    @Override
    public SegmentPerformanceAccumulator add(
            SegmentPerformanceInput input,
            SegmentPerformanceAccumulator accumulator
    ) {
        if (accumulator.getSegmentId() == null) {
            accumulator.setSegmentId(input.getSegmentId());
        }

        if (accumulator.getCampaignId() == null) {
            accumulator.setCampaignId(input.getCampaignId());
        }

        if (input.getType() == SegmentPerformanceInputType.IMPRESSION) {
            accumulator.setImpressionCount(accumulator.getImpressionCount() + 1);
        }

        if (input.getType() == SegmentPerformanceInputType.CLICK) {
            accumulator.setClickCount(accumulator.getClickCount() + 1);

            if (input.getTotalClickCost() != null) {
                accumulator.setTotalClickCost(
                        accumulator.getTotalClickCost().add(input.getTotalClickCost())
                );
            }
        }

        if (input.getType() == SegmentPerformanceInputType.CONVERSION) {
            accumulator.setConversionCount(accumulator.getConversionCount() + 1);

            if (input.getTotalConversionAmount() != null) {
                accumulator.setTotalConversionAmount(
                        accumulator.getTotalConversionAmount().add(input.getTotalConversionAmount())
                );
            }
        }

        return accumulator;
    }

    @Override
    public SegmentPerformanceAccumulator getResult(SegmentPerformanceAccumulator accumulator) {
        return accumulator;
    }

    @Override
    public SegmentPerformanceAccumulator merge(
            SegmentPerformanceAccumulator a,
            SegmentPerformanceAccumulator b
    ) {
        if (a.getSegmentId() == null) {
            a.setSegmentId(b.getSegmentId());
        }

        if (a.getCampaignId() == null) {
            a.setCampaignId(b.getCampaignId());
        }

        a.setImpressionCount(a.getImpressionCount() + b.getImpressionCount());
        a.setClickCount(a.getClickCount() + b.getClickCount());
        a.setConversionCount(a.getConversionCount() + b.getConversionCount());
        a.setTotalClickCost(a.getTotalClickCost().add(b.getTotalClickCost()));
        a.setTotalConversionAmount(a.getTotalConversionAmount().add(b.getTotalConversionAmount()));

        return a;
    }
}