package com.hyuk.settlement.streams;

import com.hyuk.settlement.shared.BudgetGateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/budget")
@Slf4j
public class BudgetQueryController {
    private final BudgetQueryService budgetQueryService;

    @GetMapping("/{campaignId}")
    public ResponseEntity<BigDecimal> getRemainingBudget(@PathVariable("campaignId") String campaignId) {
        return ResponseEntity.ok(budgetQueryService.getRemainingBudget(campaignId));
    }

    @PostMapping("/can-serve/batch")
    public ResponseEntity<Map<String, Boolean>> canServeAds(@RequestBody List<BudgetGateRequest> requests) {
        return ResponseEntity.ok(budgetQueryService.canServeAds(requests));
    }
}
