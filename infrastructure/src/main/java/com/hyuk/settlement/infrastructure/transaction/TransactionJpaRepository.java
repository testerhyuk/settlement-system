package com.hyuk.settlement.infrastructure.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {
    boolean existsByExternalTransactionId(String externalTransactionId);
    List<TransactionEntity> findByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);

    @Query("SELECT DISTINCT t.merchantId FROM TransactionEntity t WHERE t.settlementDate = :settlementDate")
    List<String> findDistinctMerchantIdsBySettlementDate(@Param("settlementDate") LocalDate settlementDate);
}
