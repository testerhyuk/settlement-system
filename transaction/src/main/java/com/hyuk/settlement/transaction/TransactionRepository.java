package com.hyuk.settlement.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String id);
    boolean existsByExternalTransactionId(String externalTransactionId);
    List<Transaction> findByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
}
