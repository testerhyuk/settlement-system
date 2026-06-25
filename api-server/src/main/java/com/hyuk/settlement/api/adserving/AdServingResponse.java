package com.hyuk.settlement.api.adserving;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.shared.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdServingResponse {
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private BigDecimal cpcAmount;
    private Currency currency;

    public static AdServingResponse from(AdCampaign adCampaign) {
        return AdServingResponse.builder()
                .impressionId(UUID.randomUUID().toString())
                .campaignId(adCampaign.getCampaignId())
                .advertiserId(adCampaign.getAdvertiserId())
                .cpcAmount(adCampaign.getCpcAmount().getAmount())
                .currency(adCampaign.getCpcAmount().getCurrency())
                .build();
    }
}
