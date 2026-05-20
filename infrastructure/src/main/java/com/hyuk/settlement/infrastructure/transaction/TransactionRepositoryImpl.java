package com.hyuk.settlement.infrastructure.transaction;

import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
    private final TransactionJpaRepository transactionJpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity transactionEntity = TransactionEntity.from(transaction);
        transactionJpaRepository.save(transactionEntity);
        return transactionEntity.toDomain();
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return transactionJpaRepository.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public boolean existsByExternalTransactionId(String externalTransactionId) {
        return transactionJpaRepository.existsByExternalTransactionId(externalTransactionId);
    }

    @Override
    public List<Transaction> findByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate) {
        return transactionJpaRepository.findByMerchantIdAndSettlementDate(merchantId, settlementDate).stream().map(TransactionEntity::toDomain).toList();
    }

    @Override
    public List<String> findDistinctMerchantIdsBySettlementDate(LocalDate settlementDate) {
        return transactionJpaRepository.findDistinctMerchantIdsBySettlementDate(settlementDate);
    }

    @Override
    public Optional<Transaction> findByExternalTransactionId(String externalTransactionId) {
        return transactionJpaRepository.findByExternalTransactionId(externalTransactionId).map(TransactionEntity::toDomain);
    }
}
