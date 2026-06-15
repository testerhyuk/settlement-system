package com.hyuk.settlement.advertiser;

import com.hyuk.settlement.shared.Money;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdCampaign {
    private String campaignId;
    private String advertiserId;
    private Money cpcAmount;
    private Money budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private AdCampaignStatus status;

    public AdCampaign(String campaignId, String advertiserId, Money cpcAmount, Money budget, LocalDate startDate, LocalDate endDate, AdCampaignStatus status) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new IllegalArgumentException("campaign Id는 필수입니다");
        }

        if (budget.isNegative()) throw new IllegalArgumentException("예산은 0 이상이어야 합니다");

        if (advertiserId == null || advertiserId.isBlank()) {
            throw new IllegalArgumentException("advertiser Id는 필수입니다");
        }

        if (cpcAmount.isNegative()) throw new IllegalArgumentException("클릭당 광고 단가는 필수입니다");

        this.campaignId = campaignId;
        this.advertiserId = advertiserId;
        this.cpcAmount = cpcAmount;
        this.budget = budget;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public static AdCampaign create(String advertiserId, Money cpcAmount, Money budget, LocalDate endDate) {
        return new AdCampaign(
                "campaign-" + UUID.randomUUID().toString(),
                advertiserId,
                cpcAmount,
                budget,
                LocalDate.now(),
                endDate,
                AdCampaignStatus.ACTIVE
        );
    }

    public void changeStatus(AdCampaignStatus newStatus) {
        this.status = newStatus;
    }

    public void updateBudget(Money amount) {
        if (amount == null || amount.isNegative()) {
            throw new IllegalArgumentException("값은 null이거나 음수일 수 없습니다");
        }

        if (!this.budget.getCurrency().equals(amount.getCurrency())) {
            throw new IllegalArgumentException("통화가 서로 다릅니다");
        }

        if (amount.isZero()) this.status = AdCampaignStatus.BUDGET_EXHAUSTED;
        if (amount.getAmount().compareTo(BigDecimal.ZERO) > 0 && this.status.equals(AdCampaignStatus.BUDGET_EXHAUSTED)) {
            this.status = AdCampaignStatus.ACTIVE;
        }

        this.budget = amount;
    }

    public void exhaustBudget() {
        this.budget = Money.ZERO;
        this.status = AdCampaignStatus.BUDGET_EXHAUSTED;
    }

    public boolean isActiveOn(LocalDate date) {
        return this.status == AdCampaignStatus.ACTIVE
                && !date.isBefore(this.startDate)
                && !date.isAfter(this.endDate);
    }
}
