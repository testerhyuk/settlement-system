package com.hyuk.settlement.api.advertiser;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.hyuk.settlement.shared.BudgetGateRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "ad-streams", url = "${feign.ad-streams.url}")
public interface BudgetQueryClient {
    @GetMapping("/api/v1/budget/{campaignId}")
    BigDecimal getRemainingBudget(@PathVariable("campaignId") String campaignId);

    @PostMapping("/api/v1/budget/can-serve/batch")
    Map<String, Boolean> canServeAds(@RequestBody List<BudgetGateRequest> requests);
}
