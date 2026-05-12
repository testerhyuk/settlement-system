package com.hyuk.settlement.infrastructure.payout;

import com.hyuk.settlement.payout.Payout;
import com.hyuk.settlement.payout.PayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PayoutRepositoryImpl implements PayoutRepository {
    private final PayoutJapRepository payoutJpaRepository;

    @Override
    public Optional<Payout> findBySettlementId(String settlementId) {
        return payoutJpaRepository.findBySettlementId(settlementId).map(PayoutEntity::toDomain);
    }

    @Override
    public Payout save(Payout payout) {
        PayoutEntity payoutEntity = PayoutEntity.from(payout);
        payoutJpaRepository.save(payoutEntity);
        return payoutEntity.toDomain();
    }

    @Override
    public Optional<Payout> findById(String id) {
        return payoutJpaRepository.findById(id).map(PayoutEntity::toDomain);
    }
}
