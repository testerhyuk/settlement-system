package com.hyuk.settlement.analytics.segment;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UnknownSegmentEvent {
    private String userId;
    private String campaignId;
    private SegmentPerformanceEventType eventType;
    private String reason;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}