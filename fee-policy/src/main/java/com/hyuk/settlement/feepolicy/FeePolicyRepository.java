package com.hyuk.settlement.feepolicy;

import java.time.LocalDate;
import java.util.Optional;
import com.hyuk.settlement.shared.CardCompany;

public interface FeePolicyRepository {
    FeePolicy save(FeePolicy feePolicy);
    Optional<FeePolicy> findById(String id);

    Optional<FeePolicy> findActivePolicy(
            String merchantId,
            CardCompany cardCompany,
            LocalDate date
    );
}