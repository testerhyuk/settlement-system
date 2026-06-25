package com.hyuk.settlement.analytics.budget;

import com.hyuk.settlement.shared.BudgetResult;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.math.BigDecimal;

public class CampaignBudgetMetricAggregateFunction
        implements AggregateFunction<
        BudgetResult,
        CampaignBudgetMetricAccumulator,
        CampaignBudgetMetricAccumulator
        > {

    @Override
    public CampaignBudgetMetricAccumulator createAccumulator() {
        return new CampaignBudgetMetricAccumulator();
    }

    @Override
    public CampaignBudgetMetricAccumulator add(
            BudgetResult value,
            CampaignBudgetMetricAccumulator accumulator
    ) {
        accumulator.setCampaignId(value.getCampaignId());

        BigDecimal amount = value.getRequestAmount() == null
                ? BigDecimal.ZERO
                : value.getRequestAmount();

        switch (value.getResultType()) {
            case CHARGED -> {
                accumulator.setChargedCount(accumulator.getChargedCount() + 1);
                accumulator.setTotalChargedAmount(
                        accumulator.getTotalChargedAmount().add(amount)
                );
            }
            case DEDUCTED -> {
                accumulator.setDeductedCount(accumulator.getDeductedCount() + 1);
                accumulator.setTotalDeductedAmount(
                        accumulator.getTotalDeductedAmount().add(amount)
                );
            }
            case REJECTED -> {
                accumulator.setRejectedCount(accumulator.getRejectedCount() + 1);

                if (value.getReason() == BudgetResult.FailureReason.INSUFFICIENT_BUDGET) {
                    accumulator.setInsufficientBudgetCount(
                            accumulator.getInsufficientBudgetCount() + 1
                    );
                }
            }
        }

        return accumulator;
    }

    @Override
    public CampaignBudgetMetricAccumulator getResult(
            CampaignBudgetMetricAccumulator accumulator
    ) {
        return accumulator;
    }

    @Override
    public CampaignBudgetMetricAccumulator merge(
            CampaignBudgetMetricAccumulator a,
            CampaignBudgetMetricAccumulator b
    ) {
        a.setChargedCount(a.getChargedCount() + b.getChargedCount());
        a.setDeductedCount(a.getDeductedCount() + b.getDeductedCount());
        a.setRejectedCount(a.getRejectedCount() + b.getRejectedCount());

        a.setTotalChargedAmount(
                a.getTotalChargedAmount().add(b.getTotalChargedAmount())
        );
        a.setTotalDeductedAmount(
                a.getTotalDeductedAmount().add(b.getTotalDeductedAmount())
        );

        a.setInsufficientBudgetCount(
                a.getInsufficientBudgetCount() + b.getInsufficientBudgetCount()
        );

        return a;
    }
}
