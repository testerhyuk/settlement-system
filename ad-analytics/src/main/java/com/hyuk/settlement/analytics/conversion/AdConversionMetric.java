package com.hyuk.settlement.analytics.conversion;

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
public class AdConversionMetric {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private long conversionCount;
    private BigDecimal totalConversionAmount;
}
