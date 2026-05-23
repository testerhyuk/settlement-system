package com.hyuk.settlement.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdClickEvent {
    private String campaignId;
    private Money cpcAmount;
    private LocalDateTime clickedAt;
}
