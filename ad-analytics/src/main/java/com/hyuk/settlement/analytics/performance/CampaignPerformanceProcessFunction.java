package com.hyuk.settlement.analytics.performance;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CampaignPerformanceProcessFunction
        extends KeyedProcessFunction<String, CampaignPerformanceInput, CampaignPerformanceMetric> {

    private static final int RATE_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private transient ValueState<CampaignPerformanceAccumulator> accumulatorState;

    private final long emitTimeoutMs;

    public CampaignPerformanceProcessFunction(long emitTimeoutMs) {
        this.emitTimeoutMs = emitTimeoutMs;
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<CampaignPerformanceAccumulator> descriptor =
                new ValueStateDescriptor<>(
                        "campaign-performance-accumulator",
                        TypeInformation.of(new TypeHint<CampaignPerformanceAccumulator>() {})
                );

        accumulatorState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(
            CampaignPerformanceInput input,
            Context context,
            Collector<CampaignPerformanceMetric> out
    ) throws Exception {
        CampaignPerformanceAccumulator accumulator = accumulatorState.value();

        if (accumulator == null) {
            accumulator = new CampaignPerformanceAccumulator();
        }

        applyInput(input, accumulator);

        if (accumulator.getTimerTimestamp() == null) {
            long timerTimestamp = context.timerService().currentProcessingTime() + emitTimeoutMs;
            context.timerService().registerProcessingTimeTimer(timerTimestamp);
            accumulator.setTimerTimestamp(timerTimestamp);
        }

        accumulatorState.update(accumulator);
    }

    @Override
    public void onTimer(
            long timestamp,
            OnTimerContext context,
            Collector<CampaignPerformanceMetric> out
    ) throws Exception {
        CampaignPerformanceAccumulator accumulator = accumulatorState.value();

        if (accumulator == null) {
            return;
        }

        boolean partial = !isReadyToEmit(accumulator);

        out.collect(toMetric(accumulator, partial));
        accumulatorState.clear();
    }

    private void applyInput(
            CampaignPerformanceInput input,
            CampaignPerformanceAccumulator accumulator
    ) {
        if (accumulator.getCampaignId() == null) {
            accumulator.setCampaignId(input.getCampaignId());
        }

        if (accumulator.getWindowStart() == null) {
            accumulator.setWindowStart(input.getWindowStart());
        }

        if (accumulator.getWindowEnd() == null) {
            accumulator.setWindowEnd(input.getWindowEnd());
        }

        if (input.getType() == CampaignPerformanceInputType.BUDGET) {
            accumulator.setBudgetReceived(true);
            if (input.getTotalDeductedAmount() != null) {
                accumulator.setTotalDeductedAmount(
                        accumulator.getTotalDeductedAmount().add(input.getTotalDeductedAmount())
                );
            }
        }

        if (input.getType() == CampaignPerformanceInputType.IMPRESSION) {
            accumulator.setImpressionReceived(true);
            if (input.getImpressionCount() != null) {
                accumulator.setImpressionCount(
                        accumulator.getImpressionCount() + input.getImpressionCount()
                );
            }
        }

        if (input.getType() == CampaignPerformanceInputType.CLICK) {
            accumulator.setClickReceived(true);
            if (input.getClickCount() != null) {
                accumulator.setClickCount(
                        accumulator.getClickCount() + input.getClickCount()
                );
            }
        }

        if (input.getType() == CampaignPerformanceInputType.CONVERSION) {
            accumulator.setConversionReceived(true);
            if (input.getConversionCount() != null) {
                accumulator.setConversionCount(
                        accumulator.getConversionCount() + input.getConversionCount()
                );
            }
            if (input.getTotalConversionAmount() != null) {
                accumulator.setTotalConversionAmount(
                        accumulator.getTotalConversionAmount().add(input.getTotalConversionAmount())
                );
            }
        }
    }

    private CampaignPerformanceMetric toMetric(CampaignPerformanceAccumulator accumulator, boolean partial) {
        BigDecimal totalDeductedAmount = accumulator.getTotalDeductedAmount() == null
                ? BigDecimal.ZERO
                : accumulator.getTotalDeductedAmount();

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
        if (totalDeductedAmount.compareTo(BigDecimal.ZERO) > 0) {
            roas = totalConversionAmount.divide(totalDeductedAmount, RATE_SCALE, ROUNDING_MODE);
        }

        return CampaignPerformanceMetric.builder()
                .campaignId(accumulator.getCampaignId())
                .windowStart(accumulator.getWindowStart())
                .windowEnd(accumulator.getWindowEnd())
                .impressionCount(accumulator.getImpressionCount())
                .clickCount(accumulator.getClickCount())
                .conversionCount(accumulator.getConversionCount())
                .totalDeductedAmount(totalDeductedAmount)
                .totalConversionAmount(totalConversionAmount)
                .ctr(ctr)
                .cvr(cvr)
                .roas(roas)
                .partial(partial)
                .build();
    }

    private boolean isReadyToEmit(CampaignPerformanceAccumulator accumulator) {
        return accumulator.isBudgetReceived()
                && accumulator.isImpressionReceived()
                && accumulator.isClickReceived();
    }
}