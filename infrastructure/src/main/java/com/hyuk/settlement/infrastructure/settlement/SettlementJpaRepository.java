package com.hyuk.settlement.infrastructure.settlement;

import com.hyuk.settlement.settlement.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementJpaRepository extends JpaRepository<SettlementEntity, String> {
    Optional<SettlementEntity> findByMerchantIdAndTargetDate(String merchantId, LocalDate targetDate);
    List<SettlementEntity> findByStatus(Status status);
}
