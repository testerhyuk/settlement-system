package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.AdCampaignStatus;
import com.hyuk.settlement.shared.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdCampaignResponse {
    private String campaignId;
    private String advertiserId;
    private Money cpcAmount;
    private Money budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private AdCampaignStatus status;
}
