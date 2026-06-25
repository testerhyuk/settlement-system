package com.hyuk.settlement.analytics.conversion;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdConversionMetricAccumulator {
    private String campaignId;
    private long conversionCount;
    private BigDecimal totalConversionAmount = BigDecimal.ZERO;
}
