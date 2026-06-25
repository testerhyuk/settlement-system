package com.hyuk.settlement.analytics.click;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
public class AdClickMetricAccumulator {
    private String campaignId;
    private long clickCount;
    private BigDecimal totalClickCost = BigDecimal.ZERO;
}
