package com.hyuk.settlement.infrastructure.feepolicy;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import com.hyuk.settlement.shared.CardCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FeePolicyRepositoryImpl implements FeePolicyRepository {
    private final FeePolicyJapRepository feePolicyJapRepository;

    @Override
    public FeePolicy save(FeePolicy feePolicy) {
        FeePolicyEntity feePolicyEntity = FeePolicyEntity.from(feePolicy);
        feePolicyJapRepository.save(feePolicyEntity);
        return feePolicyEntity.toDomain();
    }

    @Override
    public Optional<FeePolicy> findById(String id) {
        return feePolicyJapRepository.findById(id).map(FeePolicyEntity::toDomain);
    }

    @Override
    public Optional<FeePolicy> findActivePolicy(String merchantId, CardCompany cardCompany, LocalDate date) {
        return feePolicyJapRepository.findActivePolicy(merchantId, cardCompany, date).map(FeePolicyEntity::toDomain);
    }
}
