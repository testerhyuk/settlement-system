package com.hyuk.settlement.analytics.impression;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdImpressionMetricAccumulator {
    private String campaignId;
    private long impressionCount;
}
