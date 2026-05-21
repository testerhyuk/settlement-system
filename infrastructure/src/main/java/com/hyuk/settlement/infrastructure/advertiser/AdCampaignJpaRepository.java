package com.hyuk.settlement.infrastructure.advertiser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdCampaignJpaRepository extends JpaRepository<AdCampaignEntity, String> {
    @Query("SELECT ac FROM AdCampaignEntity ac WHERE ac.campaignId = :campaignId AND :currentDate BETWEEN ac.startDate AND ac.endDate AND ac.status = 'ACTIVE'")
    Optional<AdCampaignEntity> findActiveCampaign(@Param("campaignId") String campaignId, @Param("currentDate") LocalDate currentDate);

    Optional<AdCampaignEntity> findByAdvertiserIdAndCampaignId(String advertiserId, String campaignId);
    List<AdCampaignEntity> findByEndDate(LocalDate endDate);
    List<AdCampaignEntity> findAllByAdvertiserId(String advertiserId);
}
