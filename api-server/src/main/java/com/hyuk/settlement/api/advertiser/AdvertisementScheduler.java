package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.advertiser.AdCampaignStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdvertisementScheduler {
    private final AdvertiserService advertiserService;
    private final AdCampaignRepository adCampaignRepository;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void checkCampaignDate() {
        LocalDate today = LocalDate.now();

        List<AdCampaign> campaigns = advertiserService.getExpireDate(today);

        campaigns.forEach(campaign -> {
            campaign.changeStatus(AdCampaignStatus.ENDED);
            adCampaignRepository.save(campaign);
        });
    }
}
