package com.hyuk.settlement.shared;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdConversionEvent {
    @Builder.Default
    private String conversionId = UUID.randomUUID().toString();
    private String clickId;
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
    private BigDecimal conversionAmount;
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
