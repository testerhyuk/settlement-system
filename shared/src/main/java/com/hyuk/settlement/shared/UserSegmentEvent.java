package com.hyuk.settlement.shared;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSegmentEvent {
    private String userId;
    @Builder.Default
    private List<String> segmentIds = new ArrayList<>();
    @Builder.Default
    private Instant updatedAt = Instant.now();
    private boolean bootstrapComplete;
}
