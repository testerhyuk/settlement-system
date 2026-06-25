package com.hyuk.settlement.analytics.performance;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CampaignPerformanceAccumulator {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private long impressionCount;
    private long clickCount;
    private long conversionCount;

    private BigDecimal totalDeductedAmount = BigDecimal.ZERO;
    private BigDecimal totalConversionAmount = BigDecimal.ZERO;

    private boolean budgetReceived;
    private boolean impressionReceived;
    private boolean clickReceived;
    private boolean conversionReceived;
    private Long timerTimestamp;
}
