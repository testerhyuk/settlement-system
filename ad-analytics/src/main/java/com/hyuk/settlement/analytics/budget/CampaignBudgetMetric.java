package com.hyuk.settlement.analytics.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignBudgetMetric {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Long chargedCount;
    private Long deductedCount;
    private Long rejectedCount;
    private BigDecimal totalChargedAmount;
    private BigDecimal totalDeductedAmount;
    private Long insufficientBudgetCount;
    private Double rejectRate;
}
