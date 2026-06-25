package com.hyuk.settlement.api.adserving;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSegmentRequest {
    private String userId;
    private List<String> segmentIds;
}
