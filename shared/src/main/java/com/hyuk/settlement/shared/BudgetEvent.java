package com.hyuk.settlement.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    private String campaignId;
    private BigDecimal amount;
    private Currency currency;
    private BudgetEventType type;
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    public enum BudgetEventType {
        CHARGE, DEDUCT
    }
}
