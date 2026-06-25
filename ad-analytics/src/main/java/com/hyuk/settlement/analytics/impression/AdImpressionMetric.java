package com.hyuk.settlement.analytics.impression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdImpressionMetric {
    private String campaignId;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Long impressionCount;
}
