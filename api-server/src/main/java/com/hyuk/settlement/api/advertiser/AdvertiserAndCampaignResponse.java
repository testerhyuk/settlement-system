package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.AdCampaignStatus;
import com.hyuk.settlement.shared.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdvertiserAndCampaignResponse {
    private String advertiserId;
    private String brand;
    private List<AdCampaignInfo> campaign;

    @Getter
    @Builder
    public static class AdCampaignInfo {
        private String campaignId;
        private Money cpcAmount;
        private Money budget;
        private LocalDate startDate;
        private LocalDate endDate;
        private AdCampaignStatus status;
    }
}
