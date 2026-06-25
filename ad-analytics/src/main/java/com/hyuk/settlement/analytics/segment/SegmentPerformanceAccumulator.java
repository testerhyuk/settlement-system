package com.hyuk.settlement.analytics.segment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SegmentPerformanceAccumulator {
    private String segmentId;
    private String campaignId;

    private long impressionCount;
    private long clickCount;
    private long conversionCount;

    private BigDecimal totalClickCost = BigDecimal.ZERO;
    private BigDecimal totalConversionAmount = BigDecimal.ZERO;
}