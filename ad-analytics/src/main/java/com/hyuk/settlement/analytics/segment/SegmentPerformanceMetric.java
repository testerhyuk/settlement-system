package com.hyuk.settlement.analytics.segment;

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
public class SegmentPerformanceMetric {
    private String segmentId;
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private long impressionCount;
    private long clickCount;
    private long conversionCount;

    private BigDecimal totalDeductedAmount;
    private BigDecimal totalConversionAmount;

    private double ctr;
    private double cvr;
    private BigDecimal roas;

    private boolean partial;
}
