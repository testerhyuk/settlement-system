package com.hyuk.settlement.api.adserving;

import com.hyuk.settlement.advertiser.AdCampaign;
import com.hyuk.settlement.advertiser.AdCampaignRepository;
import com.hyuk.settlement.api.advertiser.BudgetQueryClient;
import com.hyuk.settlement.shared.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdServingService {
    private final AdCampaignRepository adCampaignRepository;
    private final BudgetQueryClient budgetQueryClient;
    private final AdImpressionEventProducer adImpressionEventProducer;
    private final AdClickEventProducer adClickEventProducer;
    private final AdConversionEventProducer adConversionEventProducer;
    private final UserSegmentEventProducer userSegmentEventProducer;

    public void recordUserSegments(List<UserSegmentRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("requestList 리스트는 필수입니다");
        }

        List<UserSegmentEvent> eventList = requestList.stream()
                .map(request -> {
                    if (request == null) {
                        throw new IllegalArgumentException("request 값은 필수입니다");
                    }
                    if (request.getUserId() == null || request.getUserId().isBlank()) {
                        throw new IllegalArgumentException("userId 값은 필수입니다");
                    }

                    if (request.getSegmentIds() == null || request.getSegmentIds().isEmpty()) {
                        throw new IllegalArgumentException("segmentId 값은 필수입니다");
                    }

                    request.getSegmentIds().forEach(segmentId -> {
                        if (segmentId == null || segmentId.isBlank()) {
                            throw new IllegalArgumentException("segmentId 값은 필수입니다");
                        }
                    });

                    return UserSegmentEvent.builder()
                            .userId(request.getUserId())
                            .segmentIds(request.getSegmentIds())
                            .build();
                }).toList();

        userSegmentEventProducer.sendAll(eventList);
    }

    public void recordConversions(List<AdConversionRequest> requestList) {
        validateTrackingRequests(requestList);

        List<AdConversionEvent> eventList = requestList.stream()
                .map(request -> {
                    if (request.getClickId() == null || request.getClickId().isBlank()) {
                        throw new IllegalArgumentException("ClickID는 필수값입니다");
                    }

                    if (request.getConversionAmount() == null ||request.getConversionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("ConversionAmount는 0보다 큰 값이어야 합니다");
                    }

                    return AdConversionEvent.builder()
                            .clickId(request.getClickId())
                            .impressionId(request.getImpressionId())
                            .campaignId(request.getCampaignId())
                            .advertiserId(request.getAdvertiserId())
                            .userId(request.getUserId())
                            .conversionAmount(request.getConversionAmount())
                            .build();
                }).toList();

        adConversionEventProducer.sendAll(eventList);
    }

    public void recordClicks(List<AdClickRequest> requestList) {
        validateTrackingRequests(requestList);

        List<AdClickEvent> eventList = requestList.stream()
                .map(request -> {
                    if (request.getClickId() == null || request.getClickId().isBlank()) {
                        throw new IllegalArgumentException("ClickId는 필수값입니다");
                    }

                    if (request.getCpcAmount() == null || request.getCpcAmount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("CpcAmount는 0 이상이어야 합니다");
                    }

                    if (request.getCurrency() == null) {
                        throw new IllegalArgumentException("Currency는 필수값입니다");
                    }

                    return AdClickEvent.builder()
                            .clickId(request.getClickId())
                            .impressionId(request.getImpressionId())
                            .campaignId(request.getCampaignId())
                            .advertiserId(request.getAdvertiserId())
                            .userId(request.getUserId())
                            .cpcAmount(request.getCpcAmount())
                            .currency(request.getCurrency())
                            .build();
                }).toList();

        adClickEventProducer.sendAll(eventList);
    }

    public void recordImpressions(List<AdImpressionRequest> requestList) {
        validateTrackingRequests(requestList);

        List<AdImpressionEvent> eventList = requestList.stream()
                .map(request -> {
                    return AdImpressionEvent.builder()
                            .impressionId(request.getImpressionId())
                            .campaignId(request.getCampaignId())
                            .advertiserId(request.getAdvertiserId())
                            .userId(request.getUserId())
                            .build();
                }).toList();

        adImpressionEventProducer.sendAll(eventList);
    }

    public List<AdServingResponse> adServingCandidates(String userId) {
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

    private void validateTrackingRequest(AdTrackingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request는 필수값입니다");
        }

        if (request.getCampaignId() == null || request.getCampaignId().isBlank()) {
            throw new IllegalArgumentException("CampaignId는 필수값입니다");
        }

        if (request.getImpressionId() == null || request.getImpressionId().isBlank()) {
            throw new IllegalArgumentException("ImpressionId는 필수값입니다");
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("UserId는 필수값입니다");
        }

        if (request.getAdvertiserId() == null || request.getAdvertiserId().isBlank()) {
            throw new IllegalArgumentException("AdvertiserId는 필수값입니다");
        }
    }

    private void validateTrackingRequests(List<? extends AdTrackingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("requestList는 비어있을 수 없습니다");
        }

        requests.forEach(this::validateTrackingRequest);
    }
}
