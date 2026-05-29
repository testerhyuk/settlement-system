package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.*;
import com.hyuk.settlement.infrastructure.cache.CampaignCacheService;
import com.hyuk.settlement.shared.BudgetEvent;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertiserService {
    private final AdvertiserRepository advertiserRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final BudgetQueryClient budgetQueryClient;
    private final BudgetEventProducer budgetEventProducer;
    private final CampaignCacheService campaignCacheService;

    @Transactional
    public AdvertiserResponse registerAdvertiser(String brand) {
        Advertiser advertiser = Advertiser.create(brand);

        advertiserRepository.save(advertiser);

        return AdvertiserResponse.builder()
                .advertiserId(advertiser.getAdvertiserId())
                .brand(advertiser.getBrand())
                .build();
    }

    @Transactional
    public AdCampaignResponse registerAdCampaign(RegisterAdCampaignRequest request) {
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new IllegalArgumentException("광고주가 없습니다"));

        AdCampaign campaign = AdCampaign.create(
                advertiser.getAdvertiserId(),
                new Money(request.getCpcAmount(), request.getCurrency()),
                new Money(BigDecimal.ZERO, request.getCurrency()),
                request.getEndDate()
        );

        adCampaignRepository.save(campaign);
        campaignCacheService.save(campaign);

        // 초기 예산 CHARGE 이벤트 발행
        BudgetEvent event = BudgetEvent.builder()
                .campaignId(campaign.getCampaignId())
                .amount(request.getBudget())
                .currency(request.getCurrency())
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        budgetEventProducer.send(event);

        return AdCampaignResponse.builder()
                .campaignId(campaign.getCampaignId())
                .advertiserId(campaign.getAdvertiserId())
                .cpcAmount(campaign.getCpcAmount())
                .budget(new Money(request.getBudget(), request.getCurrency()))
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .build();
    }

    public void processAdClick(String campaignId) {
        AdCampaign campaign = campaignCacheService.findById(campaignId)
                .orElseGet(() -> {
                    AdCampaign fromDb = adCampaignRepository.findById(campaignId)
                            .orElseThrow(() -> new IllegalArgumentException("캠페인이 존재하지 않습니다"));
                    campaignCacheService.save(fromDb);
                    return fromDb;
                });

        if (!campaign.isActiveOn(LocalDate.now())) {
            throw new IllegalStateException("캠페인 상태가 " + campaign.getStatus() + "입니다");
        }

        BudgetEvent event = BudgetEvent.builder()
                .campaignId(campaignId)
                .amount(campaign.getCpcAmount().getAmount())
                .currency(campaign.getCpcAmount().getCurrency())
                .type(BudgetEvent.BudgetEventType.DEDUCT)
                .build();

        budgetEventProducer.send(event);
    }

    @Transactional
    public List<AdCampaign> getExpireDate(LocalDate date) {
        return adCampaignRepository.findByEndDate(date);
    }

    @Transactional(readOnly = true)
    public AdvertiserAndCampaignResponse findAll(String advertiserId) {
        Advertiser advertiser = advertiserRepository.findById(advertiserId).orElseThrow(
                () -> new IllegalArgumentException("해당 광고주가 없습니다")
        );

        List<AdCampaign> campaigns = adCampaignRepository.findAllByAdvertiserId(advertiserId);

        List<AdvertiserAndCampaignResponse.AdCampaignInfo> campaignInfoList = campaigns.stream()
                .map(campaign -> {
                    BigDecimal remainingBudget = budgetQueryClient.getRemainingBudget(campaign.getCampaignId());

                    Money budget = remainingBudget != null
                            ? new Money(remainingBudget, campaign.getBudget().getCurrency())
                            : campaign.getBudget();

                    return AdvertiserAndCampaignResponse.AdCampaignInfo.builder()
                            .campaignId(campaign.getCampaignId())
                            .cpcAmount(campaign.getCpcAmount())
                            .budget(budget)
                            .startDate(campaign.getStartDate())
                            .endDate(campaign.getEndDate())
                            .status(campaign.getStatus())
                            .build();
                })
                .toList();

        return AdvertiserAndCampaignResponse.builder()
                .advertiserId(advertiser.getAdvertiserId())
                .brand(advertiser.getBrand())
                .campaign(campaignInfoList)
                .build();
    }

    @Transactional(readOnly = true)
    public AdCampaignResponse findByCampaignId(String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId).orElseThrow(
                () -> new IllegalArgumentException("해당 캠페인이 없습니다")
        );

        BigDecimal remainingBudget = budgetQueryClient.getRemainingBudget(campaignId);
        Money budget = remainingBudget != null
                ? new Money(remainingBudget, campaign.getBudget().getCurrency())
                : campaign.getBudget();

        return AdCampaignResponse.builder()
                .campaignId(campaign.getCampaignId())
                .advertiserId(campaign.getAdvertiserId())
                .cpcAmount(campaign.getCpcAmount())
                .budget(budget)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .build();
    }

    @Transactional
    public void pauseAdCampaign(String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId).orElseThrow(
                () -> new IllegalArgumentException("해당 캠페인이 없습니다")
        );

        if (campaign.getStatus() == AdCampaignStatus.ACTIVE) {
            campaign.changeStatus(AdCampaignStatus.PAUSED);
            adCampaignRepository.save(campaign);
            campaignCacheService.evict(campaignId);
        }
    }

    @Transactional
    public void resumeAdCampaign(String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId).orElseThrow(
                () -> new IllegalArgumentException("해당 캠페인이 없습니다")
        );

        if (campaign.getStatus() == AdCampaignStatus.PAUSED) {
            campaign.changeStatus(AdCampaignStatus.ACTIVE);
            adCampaignRepository.save(campaign);
            campaignCacheService.evict(campaignId);
        }
    }

    @Transactional
    public void chargeBudget(ChargeBudgetRequest request) {
        AdCampaign adCampaign = adCampaignRepository.findByAdvertiserIdAndCampaignId(request.getAdvertiserId(), request.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("광고주 혹은 캠페인이 없습니다"));

        BudgetEvent event = BudgetEvent.builder()
                .campaignId(request.getCampaignId())
                .amount(request.getBudget())
                .currency(request.getCurrency())
                .type(BudgetEvent.BudgetEventType.CHARGE)
                .build();

        budgetEventProducer.send(event);
    }
}
