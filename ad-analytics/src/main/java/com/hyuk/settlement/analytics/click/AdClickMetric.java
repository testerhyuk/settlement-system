package com.hyuk.settlement.analytics.click;

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
public class AdClickMetric {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private long clickCount;
    private BigDecimal totalClickCost;
}
