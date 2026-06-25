package com.hyuk.settlement.api.adserving;

import com.hyuk.settlement.shared.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdClickRequest implements AdTrackingRequest {
    private String clickId;
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
    private BigDecimal cpcAmount;
    private Currency currency;
}
