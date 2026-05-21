package com.hyuk.settlement.infrastructure.advertiser;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignStatus;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_campaign")
public class AdCampaignEntity {
    @Id
    private String campaignId;
    private String advertiserId;
    private BigDecimal cpcAmount;
    private BigDecimal budget;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private AdCampaignStatus status;

    public static AdCampaignEntity from(AdCampaign adCampaign) {
        AdCampaignEntity adCampaignEntity = new AdCampaignEntity();
        adCampaignEntity.campaignId = adCampaign.getCampaignId();
        adCampaignEntity.advertiserId = adCampaign.getAdvertiserId();
        adCampaignEntity.cpcAmount = adCampaign.getCpcAmount().getAmount();
        adCampaignEntity.budget = adCampaign.getBudget().getAmount();
        adCampaignEntity.currency = adCampaign.getCpcAmount().getCurrency();
        adCampaignEntity.startDate = adCampaign.getStartDate();
        adCampaignEntity.endDate = adCampaign.getEndDate();
        adCampaignEntity.status = adCampaign.getStatus();

        return adCampaignEntity;
    }

    public AdCampaign toAdCampaign() {
        return new AdCampaign(
                campaignId,
                advertiserId,
                new Money(cpcAmount, currency),
                new Money(budget, currency),
                startDate,
                endDate,
                status
        );
    }
}
