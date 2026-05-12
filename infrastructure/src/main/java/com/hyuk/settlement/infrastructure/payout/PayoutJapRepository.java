package com.hyuk.settlement.infrastructure.payout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutJapRepository extends JpaRepository<PayoutEntity, String> {
    Optional<PayoutEntity> findBySettlementId(String settlementId);
}