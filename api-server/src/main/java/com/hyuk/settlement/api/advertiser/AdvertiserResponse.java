package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.shared.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdvertiserResponse {
    private String advertiserId;
    private String brand;
}
