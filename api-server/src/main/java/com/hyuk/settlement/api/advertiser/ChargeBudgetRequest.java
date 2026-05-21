package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.shared.Currency;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ChargeBudgetRequest {
    private String advertiserId;
    private String campaignId;
    private BigDecimal budget;
    private Currency currency;
}
