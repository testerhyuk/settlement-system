package com.hyuk.settlement.payout;

import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hyuk.settlement.shared.BankAccount;

@Getter
public class Payout {
    private String payoutId;
    private String settlementId;
    private String merchantId;
    private Money amount;
    private BankAccount bankAccount;
    private Status status;
    private Integer attemptCount;
    private FailureType failureType;
    private String failureReason;
    private LocalDateTime completedAt;

    public Payout(String payoutId, String settlementId,  String merchantId, Money amount, BankAccount bankAccount,
                  Status status, Integer attemptCount, FailureType failureType, String failureReason, LocalDateTime completedAt) {
        if (payoutId == null || payoutId.isBlank()) throw new IllegalArgumentException("payoutId는 필수입니다");
        if (settlementId == null || settlementId.isBlank()) throw new IllegalArgumentException("settlementId는 필수입니다");
        if (merchantId == null || merchantId.isBlank()) throw new IllegalArgumentException("merchantId는 필수입니다");
        if (amount == null) throw new IllegalArgumentException("금액 정보는 필수입니다");
        if (bankAccount == null) throw new IllegalArgumentException("계좌 정보는 필수입니다");
        if (status == null) throw new IllegalArgumentException("송금 상태 정보는 필수입니다");
        if (attemptCount == null) throw new IllegalArgumentException("재시도 횟수는 필수입니다");
        if (attemptCount < 0) throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다");
        if (attemptCount > 3) throw new IllegalArgumentException("재시도 횟수는 최대 3회입니다");

        this.payoutId = payoutId;
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.bankAccount = bankAccount;
        this.status = status;
        this.attemptCount = attemptCount;
        this.failureType = failureType;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
    }

    public static Payout create(String settlementId, String merchantId, Money amount, BankAccount bankAccount, Status status) {
        return new Payout(
                "payout-" + UUID.randomUUID().toString(),
                settlementId,
                merchantId,
                amount,
                bankAccount,
                status,
                0,
                null,
                null,
                null
        );
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void attemptCountPlusOne() {
        this.attemptCount++;
    }

    public void updateFailureType(FailureType failureType) {
        this.failureType = failureType;
    }
}
