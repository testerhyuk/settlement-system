package com.hyuk.settlement.api.feepolicy;

import com.hyuk.settlement.shared.CardCompany;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class FeePolicyResponse {
    private String feePolicyId;
    private String merchantId;
    private CardCompany cardCompany;
    private BigDecimal feeRate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
