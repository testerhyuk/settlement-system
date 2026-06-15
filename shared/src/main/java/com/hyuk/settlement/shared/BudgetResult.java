package com.hyuk.settlement.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResult {
    private String campaignId;
    private String eventId;
    private BigDecimal requestAmount;
    private BigDecimal remainingBudget;
    private Currency currency;
    private BudgetResultType resultType;
    private FailureReason reason;
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    public enum BudgetResultType {
        CHARGED,
        DEDUCTED,
        REJECTED
    }

    public enum FailureReason {
        INSUFFICIENT_BUDGET,
        BUDGET_STATE_NOT_FOUND,
        INVALID_BUDGET_STATE
    }
}
