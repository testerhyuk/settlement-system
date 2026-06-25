package com.hyuk.settlement.api.adserving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdConversionRequest implements AdTrackingRequest {
    private String clickId;
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
    private BigDecimal conversionAmount;
}
