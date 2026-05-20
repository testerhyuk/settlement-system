package com.hyuk.settlement.infrastructure.transaction;

import com.hyuk.settlement.shared.CardCompany;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transaction")
public class TransactionEntity {
    @Id
    private String transactionId;
    @Column(unique = true)
    private String externalTransactionId;
    private String originalTransactionId;
    private String merchantId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    private CardCompany cardCompany;
    private LocalDateTime approvedAt;
    private LocalDate settlementDate;

    public static TransactionEntity from(Transaction transaction) {
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.transactionId = transaction.getTransactionId();
        transactionEntity.externalTransactionId = transaction.getExternalTransactionId();
        transactionEntity.originalTransactionId = transaction.getOriginalTransactionId();
        transactionEntity.merchantId = transaction.getMerchantId();
        transactionEntity.amount = transaction.getAmount().getAmount();
        transactionEntity.currency = transaction.getAmount().getCurrency();
        transactionEntity.transactionType = transaction.getTransactionType();
        transactionEntity.cardCompany = transaction.getCardCompany();
        transactionEntity.approvedAt = transaction.getApprovedAt();
        transactionEntity.settlementDate = transaction.getSettlementDate();

        return transactionEntity;
    }

    public Transaction toDomain() {
        return new Transaction(
                transactionId,
                externalTransactionId,
                originalTransactionId,
                merchantId,
                new Money(amount, currency),
                transactionType,
                cardCompany,
                approvedAt,
                settlementDate
        );
    }
}
