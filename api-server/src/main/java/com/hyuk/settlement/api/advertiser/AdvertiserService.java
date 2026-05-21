package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.*;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertiserService {
    private final AdvertiserRepository advertiserRepository;
    private final AdCampaignRepository adCampaignRepository;

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
    public void chargeBudget(ChargeBudgetRequest request) {
        AdCampaign adCampaign = adCampaignRepository.findByAdvertiserIdAndCampaignId(request.getAdvertiserId(), request.getCampaignId())
                        .orElseThrow(() -> new IllegalArgumentException("광고주 혹은 캠페인이 없습니다"));

        adCampaign.chargeBudget(new Money(request.getBudget(), request.getCurrency()));

        adCampaignRepository.save(adCampaign);
    }

    @Transactional
    public AdCampaignResponse registerAdCampaign(RegisterAdCampaignRequest request) {
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new IllegalArgumentException("광고주가 없습니다"));

        AdCampaign campaign = AdCampaign.create(
                advertiser.getAdvertiserId(),
                new Money(request.getCpcAmount(), request.getCurrency()),
                new Money(request.getBudget(), request.getCurrency()),
                request.getEndDate()
        );

        adCampaignRepository.save(campaign);

        return AdCampaignResponse.builder()
                .campaignId(campaign.getCampaignId())
                .advertiserId(campaign.getAdvertiserId())
                .cpcAmount(campaign.getCpcAmount())
                .budget(campaign.getBudget())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .build();
    }

    @Transactional
    public void processAdClick(String campaignId) {
        AdCampaign campaign = adCampaignRepository.findActiveCampaign(campaignId, LocalDate.now())
                .orElseGet(() -> {
                    AdCampaign found = adCampaignRepository.findById(campaignId)
                            .orElseThrow(() -> new IllegalArgumentException("캠페인이 존재하지 않습니다"));

                    throw new IllegalStateException("캠페인 상태가 " + found.getStatus() + "입니다");
                });

        campaign.deductBudget(campaign.getCpcAmount());

        adCampaignRepository.save(campaign);
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
                .map(campaign -> AdvertiserAndCampaignResponse.AdCampaignInfo.builder()
                        .campaignId(campaign.getCampaignId())
                        .cpcAmount(campaign.getCpcAmount())
                        .budget(campaign.getBudget())
                        .startDate(campaign.getStartDate())
                        .endDate(campaign.getEndDate())
                        .status(campaign.getStatus())
                        .build())
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

        return AdCampaignResponse.builder()
                .campaignId(campaign.getCampaignId())
                .advertiserId(campaign.getAdvertiserId())
                .cpcAmount(campaign.getCpcAmount())
                .budget(campaign.getBudget())
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
        }
    }
}
