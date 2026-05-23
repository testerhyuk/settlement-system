package com.hyuk.settlement.streams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

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
}
