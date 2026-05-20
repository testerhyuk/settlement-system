package com.hyuk.settlement.infrastructure.settlement;

import com.hyuk.settlement.settlement.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementJpaRepository extends JpaRepository<SettlementEntity, String> {
    List<SettlementEntity> findByMerchantIdAndStatus(String merchantId, Status status);
    List<SettlementEntity> findByStatus(Status status);
    boolean existsByMerchantIdAndStatusAndTargetDate(String merchantId, Status status, LocalDate targetDate);
}
