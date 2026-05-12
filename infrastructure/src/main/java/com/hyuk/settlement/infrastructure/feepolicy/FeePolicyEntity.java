package com.hyuk.settlement.infrastructure.feepolicy;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.shared.CardCompany;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fee_policy")
public class FeePolicyEntity {
    @Id
    private String feePolicyId;
    private String merchantId;
    @Enumerated(EnumType.STRING)
    private CardCompany cardCompany;
    private BigDecimal feeRate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public static FeePolicyEntity from(FeePolicy feePolicy) {
        FeePolicyEntity feePolicyEntity = new FeePolicyEntity();
        feePolicyEntity.feePolicyId = feePolicy.getFeePolicyId();
        feePolicyEntity.merchantId = feePolicy.getMerchantId();
        feePolicyEntity.cardCompany = feePolicy.getCardCompany();
        feePolicyEntity.feeRate = feePolicy.getFeeRate();
        feePolicyEntity.effectiveFrom = feePolicy.getEffectiveFrom();
        feePolicyEntity.effectiveTo = feePolicy.getEffectiveTo();

        return feePolicyEntity;
    }

    public FeePolicy toDomain() {
        return new FeePolicy(
                feePolicyId,
                merchantId,
                cardCompany,
                feeRate,
                effectiveFrom,
                effectiveTo
        );
    }
}
