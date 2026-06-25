package com.hyuk.settlement.analytics.budget;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
public class CampaignBudgetMetricAccumulator {
    private String campaignId;

    private long chargedCount;
    private long deductedCount;
    private long rejectedCount;

    private BigDecimal totalChargedAmount = BigDecimal.ZERO;
    private BigDecimal totalDeductedAmount = BigDecimal.ZERO;

    private long insufficientBudgetCount;
}
