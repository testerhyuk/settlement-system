package com.hyuk.settlement.infrastructure.payout;

import com.hyuk.settlement.payout.FailureType;
import com.hyuk.settlement.payout.Payout;
import com.hyuk.settlement.payout.Status;
import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payout")
public class PayoutEntity {
    @Id
    private String payoutId;
    private String settlementId;
    private String merchantId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Integer attemptCount;
    @Enumerated(EnumType.STRING)
    private FailureType failureType;
    private String failureReason;
    private LocalDateTime completedAt;

    public static PayoutEntity from(Payout payout) {
        PayoutEntity payoutEntity = new PayoutEntity();
        payoutEntity.payoutId = payout.getPayoutId();
        payoutEntity.settlementId = payout.getSettlementId();
        payoutEntity.merchantId = payout.getMerchantId();
        payoutEntity.amount = payout.getAmount().getAmount();
        payoutEntity.currency = payout.getAmount().getCurrency();
        payoutEntity.bankName = payout.getBankAccount().getBankName();
        payoutEntity.accountNumber = payout.getBankAccount().getAccountNumber();
        payoutEntity.accountHolder = payout.getBankAccount().getAccountHolder();
        payoutEntity.status = payout.getStatus();
        payoutEntity.attemptCount = payout.getAttemptCount();
        payoutEntity.failureType = payout.getFailureType();
        payoutEntity.failureReason = payout.getFailureReason();
        payoutEntity.completedAt = payout.getCompletedAt();
        return payoutEntity;
    }

    public Payout toDomain() {
        return new Payout(
                payoutId,
                settlementId,
                merchantId,
                new Money(amount, currency),
                new BankAccount(bankName, accountNumber, accountHolder),
                status,
                attemptCount,
                failureType,
                failureReason,
                completedAt
        );
    }
}
