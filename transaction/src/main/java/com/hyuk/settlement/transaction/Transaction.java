package com.hyuk.settlement.transaction;

import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.hyuk.settlement.shared.CardCompany;

@Getter
public class Transaction {
    private String transactionId;
    private String externalTransactionId;
    private String merchantId;
    private Money amount;
    private TransactionType transactionType;
    private CardCompany cardCompany;
    private LocalDateTime approvedAt;
    private LocalDate settlementDate;

    public Transaction(String transactionId, String externalTransactionId, String merchantId, Money amount, TransactionType transactionType,
                       CardCompany cardCompany, LocalDateTime approvedAt, LocalDate settlementDate) {
        if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("transactionId는 필수입니다");

        if (externalTransactionId == null || externalTransactionId.isBlank()) throw new IllegalArgumentException("externalTransactionId는 필수입니다");

        if (merchantId == null || merchantId.isBlank()) throw new IllegalArgumentException("merchantId는 필수입니다");

        if (amount == null) throw new IllegalArgumentException("금액 정보는 필수입니다");

        if (transactionType == null) throw new IllegalArgumentException("거래 타입 정보는 필수입니다");

        if (settlementDate == null) throw new IllegalArgumentException("정산일 정보는 필수입니다");

        if (cardCompany == null) throw new IllegalArgumentException("카드사 정보는 필수입니다");

        if (approvedAt == null) throw new IllegalArgumentException("승인 날짜 정보가 필요합니다");

        this.transactionId = transactionId;
        this.externalTransactionId = externalTransactionId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.cardCompany = cardCompany;
        this.approvedAt = approvedAt;
        this.settlementDate = settlementDate;
    }

    public static Transaction create(String externalTransactionId, String merchantId, Money amount,
                                     TransactionType transactionType, CardCompany cardCompany, LocalDateTime approvedAt, LocalDate settlementDate) {
        return new Transaction(
                "transaction-" + UUID.randomUUID().toString(),
                externalTransactionId,
                merchantId,
                amount,
                transactionType,
                cardCompany,
                approvedAt,
                settlementDate
        );
    }
}
