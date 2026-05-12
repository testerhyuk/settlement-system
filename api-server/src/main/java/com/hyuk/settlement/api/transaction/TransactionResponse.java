package com.hyuk.settlement.api.transaction;

import com.hyuk.settlement.shared.CardCompany;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String externalTransactionId;
    private String merchantId;
    private Money amount;
    private TransactionType transactionType;
    private CardCompany cardCompany;
    private LocalDateTime approvedAt;
    private LocalDate settlementDate;
}
