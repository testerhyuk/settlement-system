package com.hyuk.settlement.analytics.performance;

import com.hyuk.settlement.analytics.budget.CampaignBudgetMetric;
import com.hyuk.settlement.analytics.click.AdClickMetric;
import com.hyuk.settlement.analytics.conversion.AdConversionMetric;
import com.hyuk.settlement.analytics.impression.AdImpressionMetric;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CampaignPerformanceInput {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private Long impressionCount;
    private Long clickCount;
    private Long conversionCount;

    private BigDecimal totalDeductedAmount;
    private BigDecimal totalConversionAmount;

    private CampaignPerformanceInputType type;

    public static CampaignPerformanceInput fromBudgetMetric(CampaignBudgetMetric budgetMetric) {
        CampaignPerformanceInput input = new CampaignPerformanceInput();
        input.campaignId = budgetMetric.getCampaignId();
        input.windowStart = budgetMetric.getWindowStart();
        input.windowEnd = budgetMetric.getWindowEnd();
        input.totalDeductedAmount = budgetMetric.getTotalDeductedAmount();
        input.type = CampaignPerformanceInputType.BUDGET;

        return input;
    }

    public static CampaignPerformanceInput fromImpressionMetric(AdImpressionMetric impressionMetric) {
        CampaignPerformanceInput input = new CampaignPerformanceInput();
        input.campaignId = impressionMetric.getCampaignId();
        input.impressionCount = impressionMetric.getImpressionCount();
        input.windowStart = impressionMetric.getWindowStart();
        input.windowEnd = impressionMetric.getWindowEnd();
        input.type = CampaignPerformanceInputType.IMPRESSION;

        return input;
    }

    public static CampaignPerformanceInput fromClickMetric(AdClickMetric clickMetric) {
        CampaignPerformanceInput input = new CampaignPerformanceInput();
        input.campaignId = clickMetric.getCampaignId();
        input.clickCount = clickMetric.getClickCount();
        input.windowStart = clickMetric.getWindowStart();
        input.windowEnd = clickMetric.getWindowEnd();
        input.type = CampaignPerformanceInputType.CLICK;

        return input;
    }

    public static CampaignPerformanceInput fromConversionMetric(AdConversionMetric conversionMetric) {
        CampaignPerformanceInput input = new CampaignPerformanceInput();
        input.campaignId = conversionMetric.getCampaignId();
        input.conversionCount = conversionMetric.getConversionCount();
        input.windowStart = conversionMetric.getWindowStart();
        input.windowEnd = conversionMetric.getWindowEnd();
        input.totalConversionAmount = conversionMetric.getTotalConversionAmount();
        input.type = CampaignPerformanceInputType.CONVERSION;

        return input;
    }
}
