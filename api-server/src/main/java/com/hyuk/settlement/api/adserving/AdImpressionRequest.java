package com.hyuk.settlement.api.adserving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdImpressionRequest implements AdTrackingRequest {
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
}
