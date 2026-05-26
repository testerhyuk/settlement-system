package com.hyuk.settlement.api.advertiser;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "ad-streams", url = "${feign.ad-streams.url}")
public interface BudgetQueryClient {
    @GetMapping("/api/v1/budget/{campaignId}")
    BigDecimal getRemainingBudget(@PathVariable("campaignId") String campaignId);
}
