package com.hyuk.settlement.settlement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);
    Optional<Settlement> findById(String id);
    Optional<Settlement> findByMerchantIdAndTargetDate(String merchantId, LocalDate targetDate);
    List<Settlement> findByStatus(Status status);
}