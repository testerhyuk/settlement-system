package com.hyuk.settlement.advertiser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdCampaignRepository {
    Optional<AdCampaign> findById(String campaignId);
    Optional<AdCampaign> findByAdvertiserIdAndCampaignId(String advertiserId, String campaignId);
    Optional<AdCampaign> findActiveCampaign(String campaignId, LocalDate currentDate);
    AdCampaign save(AdCampaign adCampaign);
    List<AdCampaign> findByEndDate(LocalDate endDate);
    List<AdCampaign> findAllByAdvertiserId(String advertiserId);
    List<AdCampaign> findServingCandidates(LocalDate today);
}
