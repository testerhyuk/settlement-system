package com.hyuk.settlement.payout;

import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.time.LocalDateTime;
import com.hyuk.settlement.shared.BankAccount;

@Getter
public class Payout {
    private String payoutId;
    private String settlementId;
    private String merchantId;
    private Money amount;
    private BankAccount bankAccount;
    private Status status;
    private Integer attemptAccount;
    private FailureType failureType;
    private String failureReason;
    private LocalDateTime completedAt;

    public Payout(String payoutId, String settlementId,  String merchantId, Money amount, BankAccount bankAccount,
                  Status status, Integer attemptAccount, FailureType failureType, String failureReason, LocalDateTime completedAt) {
        if (payoutId == null || payoutId.isBlank()) throw new IllegalArgumentException("payoutId는 필수입니다");
        if (settlementId == null || settlementId.isBlank()) throw new IllegalArgumentException("settlementId는 필수입니다");
        if (merchantId == null || merchantId.isBlank()) throw new IllegalArgumentException("merchantId는 필수입니다");
        if (amount == null) throw new IllegalArgumentException("금액 정보는 필수입니다");
        if (bankAccount == null) throw new IllegalArgumentException("계좌 정보는 필수입니다");
        if (status == null) throw new IllegalArgumentException("송금 상태 정보는 필수입니다");
        if (attemptAccount == null) throw new IllegalArgumentException("재시도 횟수는 필수입니다");
        if (attemptAccount < 0) throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다");
        if (attemptAccount > 3) throw new IllegalArgumentException("재시도 횟수는 최대 3회입니다");

        this.payoutId = payoutId;
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.bankAccount = bankAccount;
        this.status = status;
        this.attemptAccount = attemptAccount;
        this.failureType = failureType;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
    }
}
