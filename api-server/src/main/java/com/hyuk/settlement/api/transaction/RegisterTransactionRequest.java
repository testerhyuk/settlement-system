package com.hyuk.settlement.api.transaction;

import com.hyuk.settlement.shared.CardCompany;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.transaction.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RegisterTransactionRequest {
    private String externalTransactionId;
    private String merchantId;
    private BigDecimal amount;
    private CardCompany cardCompany;
    private TransactionType transactionType;
    private LocalDateTime approvedAt;
}
