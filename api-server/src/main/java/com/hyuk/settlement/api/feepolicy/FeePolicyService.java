package com.hyuk.settlement.api.feepolicy;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeePolicyService {
    private final FeePolicyRepository feePolicyRepository;

    public FeePolicyResponse registerFeePolicy(RegisterFeePolicyRequest request) {
        FeePolicy feePolicy = FeePolicy.create(
                request.getMerchantId(),
                request.getCardCompany(),
                request.getFeeRate(),
                request.getEffectiveFrom(),
                request.getEffectiveTo()
        );

        feePolicyRepository.save(feePolicy);

        return FeePolicyResponse.builder()
                .feePolicyId(feePolicy.getFeePolicyId())
                .merchantId(request.getMerchantId())
                .cardCompany(request.getCardCompany())
                .feeRate(request.getFeeRate())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();
    }
}
