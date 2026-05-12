package com.hyuk.settlement.infrastructure.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {
    boolean existsByExternalTransactionId(String externalTransactionId);
    List<TransactionEntity> findByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
}
