package com.hyuk.settlement.api.adserving;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.api.advertiser.BudgetQueryClient;
import com.hyuk.settlement.shared.BudgetGateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdServingService {
    private final AdCampaignRepository adCampaignRepository;
    private final BudgetQueryClient budgetQueryClient;

    public List<AdServingResponse> adServingCandidates() {
        List<AdCampaign> adList = adCampaignRepository.findServingCandidates(LocalDate.now());

        if (adList.isEmpty()) {
            return List.of();
        }

        List<BudgetGateRequest> gateRequests = adList.stream()
                .map(ad -> BudgetGateRequest.builder()
                        .campaignId(ad.getCampaignId())
                        .cpcAmount(ad.getCpcAmount().getAmount())
                        .build())
                .toList();

        Map<String, Boolean> gateResults;

        try {
            gateResults = budgetQueryClient.canServeAds(gateRequests);
        } catch (Exception e) {
            log.warn("Budget Gate 조회 실패. 광고 후보를 반환하지 않습니다.", e);
            return List.of();
        }

        return adList.stream()
                .filter(ad -> Boolean.TRUE.equals(gateResults.get(ad.getCampaignId())))
                .map(AdServingResponse::from)
                .toList();
    }
}
