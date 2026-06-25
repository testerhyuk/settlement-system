package com.hyuk.settlement.shared;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdImpressionEvent {
    private String impressionId;
    private String campaignId;
    private String advertiserId;
    private String userId;
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
