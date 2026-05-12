package com.hyuk.settlement.payout;

import java.util.Optional;

public interface PayoutRepository {
    Optional<Payout> findBySettlementId(String settlementId);
    Payout save(Payout payout);
    Optional<Payout> findById(String id);
}
