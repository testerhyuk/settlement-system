package com.hyuk.settlement.api.advertiser;

import com.hyuk.settlement.shared.Currency;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class RegisterAdCampaignRequest {
    private String advertiserId;
    private BigDecimal cpcAmount;
    private BigDecimal budget;
    private Currency currency;
    private LocalDate endDate;
}
