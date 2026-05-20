package com.hyuk.settlement.settlement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {
    Settlement save(Settlement settlement);
    Optional<Settlement> findById(String id);
    List<Settlement> findByMerchantIdAndStatus(String merchantId, Status status);
    List<Settlement> findByStatus(Status status);
    boolean existsByMerchantIdAndStatusAndTargetDate(String merchantId, Status status, LocalDate targetDate);
}