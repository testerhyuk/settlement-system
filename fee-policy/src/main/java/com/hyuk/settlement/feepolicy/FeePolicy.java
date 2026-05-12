package com.hyuk.settlement.feepolicy;

import lombok.Getter;
import com.hyuk.settlement.shared.CardCompany;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class FeePolicy {
    private String feePolicyId;
    private String merchantId;
    private CardCompany cardCompany;
    private BigDecimal feeRate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public FeePolicy(String feePolicyId, String merchantId, CardCompany cardCompany, BigDecimal feeRate,
                     LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (feePolicyId == null || feePolicyId.isBlank()) throw new IllegalArgumentException("feePolicyId는 필수입니다");
        if (merchantId == null || merchantId.isBlank()) throw new IllegalArgumentException("merchantId는 필수입니다");
        if (cardCompany == null) throw new IllegalArgumentException("카드사 종류는 필수입니다");
        if (feeRate == null) throw new IllegalArgumentException("수수료율은 필수입니다");
        if (feeRate.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("수수료율은 0 이상이어야 합니다");
        if (feeRate.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("수수료율은 1 이하여야 합니다");
        if (effectiveFrom == null || effectiveTo == null) throw new IllegalArgumentException("정책 적용 날짜는 필수입니다");
        if (effectiveFrom.isAfter(effectiveTo)) throw new IllegalArgumentException("적용 종료일이 시작일보다 빠를 수 없습니다");

        this.feePolicyId = feePolicyId;
        this.merchantId = merchantId;
        this.cardCompany = cardCompany;
        this.feeRate = feeRate;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }
}
