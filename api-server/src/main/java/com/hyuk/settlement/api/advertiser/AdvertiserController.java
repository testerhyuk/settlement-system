package com.hyuk.settlement.api.advertiser;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/advertisement")
public class AdvertiserController {
    private final AdvertiserService advertiserService;

    @PostMapping("/advertiser/register/{brand}")
    public ResponseEntity<AdvertiserResponse> registerAdvertiser(@PathVariable("brand") String brand) {
        AdvertiserResponse response = advertiserService.registerAdvertiser(brand);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/advertiser/charge")
    public ResponseEntity<String> chargeBudget(@RequestBody ChargeBudgetRequest request) {
        advertiserService.chargeBudget(request);

        return ResponseEntity.ok("요청하신 금액이 충전되었습니다");
    }

    @PostMapping("/ad-campaign/register")
    public ResponseEntity<AdCampaignResponse> registerAdCampaign(@RequestBody RegisterAdCampaignRequest request) {
        AdCampaignResponse response = advertiserService.registerAdCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/ad-campaign/click/{campaignId}")
    public ResponseEntity<Void> processAdClick(@PathVariable("campaignId") String campaignId) {
        advertiserService.processAdClick(campaignId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/advertiser/{advertiserId}")
    public ResponseEntity<AdvertiserAndCampaignResponse> findAll(@PathVariable("advertiserId") String advertiserId) {
        AdvertiserAndCampaignResponse response = advertiserService.findAll(advertiserId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/ad-campaign/{campaignId}")
    public ResponseEntity<AdCampaignResponse> findByCampaignId(@PathVariable("campaignId") String campaignId) {
        AdCampaignResponse response = advertiserService.findByCampaignId(campaignId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/ad-campaign/{campaignId}/pause")
    public ResponseEntity<String> pauseAdCampaign(@PathVariable("campaignId") String campaignId) {
        advertiserService.pauseAdCampaign(campaignId);
        return ResponseEntity.ok("캠페인이 중지되었습니다");
    }

    @PutMapping("/ad-campaign/{campaignId}/resume")
    public ResponseEntity<String> resumeAdCampaign(@PathVariable("campaignId") String campaignId) {
        advertiserService.resumeAdCampaign(campaignId);
        return ResponseEntity.ok("캠페인이 활성화되었습니다");
    }
}