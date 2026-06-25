package com.hyuk.settlement.shared;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
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
    private Instant processedAt = Instant.now();

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
