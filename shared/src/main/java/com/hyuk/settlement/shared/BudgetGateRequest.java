package com.hyuk.settlement.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetGateRequest {
    private String campaignId;
    private BigDecimal cpcAmount;
}
