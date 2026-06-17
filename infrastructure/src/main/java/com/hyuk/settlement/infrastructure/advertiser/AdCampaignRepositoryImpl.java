package com.hyuk.settlement.infrastructure.advertiser;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdCampaignRepositoryImpl implements AdCampaignRepository {
    private final AdCampaignJpaRepository adCampaignJpaRepository;
    @Override
    public Optional<AdCampaign> findById(String campaignId) {
        return adCampaignJpaRepository.findById(campaignId).map(AdCampaignEntity::toAdCampaign);
    }

    @Override
    public Optional<AdCampaign> findActiveCampaign(String campaignId, LocalDate currentDate) {
        return adCampaignJpaRepository.findActiveCampaign(campaignId, currentDate).map(AdCampaignEntity::toAdCampaign);
    }

    @Override
    public AdCampaign save(AdCampaign adCampaign) {
        AdCampaignEntity entity = AdCampaignEntity.from(adCampaign);
        adCampaignJpaRepository.save(entity);
        return entity.toAdCampaign();
    }

    @Override
    public Optional<AdCampaign> findByAdvertiserIdAndCampaignId(String advertiserId, String campaignId) {
        return adCampaignJpaRepository.findByAdvertiserIdAndCampaignId(advertiserId, campaignId).map(AdCampaignEntity::toAdCampaign);
    }

    @Override
    public List<AdCampaign> findByEndDate(LocalDate endDate) {
        return adCampaignJpaRepository.findByEndDate(endDate).stream().map(AdCampaignEntity::toAdCampaign).toList();
    }

    @Override
    public List<AdCampaign> findAllByAdvertiserId(String advertiserId) {
        return adCampaignJpaRepository.findAllByAdvertiserId(advertiserId).stream().map(AdCampaignEntity::toAdCampaign).toList();
    }

    @Override
    public List<AdCampaign> findServingCandidates(LocalDate today) {
        return adCampaignJpaRepository.findServingCandidates(today).stream().map(AdCampaignEntity::toAdCampaign).toList();
    }
}
