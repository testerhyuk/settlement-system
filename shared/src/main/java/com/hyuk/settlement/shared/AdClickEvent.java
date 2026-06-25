package com.hyuk.settlement.shared;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdClickEvent {
    @Builder.Default
    private String clickId = UUID.randomUUID().toString();
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
    private BigDecimal cpcAmount;
    private Currency currency;
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
