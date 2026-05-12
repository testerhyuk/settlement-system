package com.hyuk.settlement.infrastructure.settlement;

import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepository {
    private final SettlementJpaRepository settlementJpaRepository;

    @Override
    public Settlement save(Settlement settlement) {
        SettlementEntity settlementEntity = SettlementEntity.from(settlement);
        settlementJpaRepository.save(settlementEntity);
        return settlementEntity.toDomain();
    }

    @Override
    public Optional<Settlement> findById(String id) {
        return settlementJpaRepository.findById(id).map(SettlementEntity::toDomain);
    }

    @Override
    public Optional<Settlement> findByMerchantIdAndTargetDate(String merchantId, LocalDate targetDate) {
        return settlementJpaRepository.findByMerchantIdAndTargetDate(merchantId, targetDate).map(SettlementEntity::toDomain);
    }

    @Override
    public List<Settlement> findByStatus(Status status) {
        return settlementJpaRepository.findByStatus(status).stream().map(SettlementEntity::toDomain).toList();
    }
}
