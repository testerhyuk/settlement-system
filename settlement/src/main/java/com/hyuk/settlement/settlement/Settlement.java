package com.hyuk.settlement.settlement;

import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

import com.hyuk.settlement.shared.SettlementCycle;

@Getter
public class Settlement {
    private String settlementId;
    private String merchantId;
    private LocalDate targetDate;
    private LocalDate payoutDate;
    private Money grossAmount;
    private Money totalFee;
    private Money netAmount;
    private Status status;
    private SettlementCycle settlementCycle;
    private Integer retryCount;

    public Settlement(String settlementId, String merchantId, LocalDate targetDate, LocalDate payoutDate,
                      Money grossAmount, Money totalFee, Money netAmount, Status status,
                      SettlementCycle settlementCycle, Integer retryCount) {
        if (settlementId == null || settlementId.isBlank()) throw new IllegalArgumentException("settlementId는 필수입니다");
        if (merchantId == null || merchantId.isBlank()) throw new IllegalArgumentException("merchantId는 필수입니다");
        if (targetDate == null) throw new IllegalArgumentException("정산 기준일은 필수입니다");
        if (payoutDate == null) throw new IllegalArgumentException("송금 예정일은 필수입니다");
        if (grossAmount == null) throw new IllegalArgumentException("총 거래액은 필수입니다");
        if (totalFee == null) throw new IllegalArgumentException("총 수수료 금액은 필수입니다");
        if (netAmount == null) throw new IllegalArgumentException("최종 지급액은 필수입니다");
        if (status == null) throw new IllegalArgumentException("정산 상태는 필수입니다");
        if (settlementCycle == null) throw new IllegalArgumentException("정산 주기는 필수입니다");

        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.targetDate = targetDate;
        this.payoutDate = payoutDate;
        this.grossAmount = grossAmount;
        this.totalFee = totalFee;
        this.netAmount = netAmount;
        this.status = status;
        this.settlementCycle = settlementCycle;
        this.retryCount = retryCount;
    }

    public static Settlement create(String merchantId, LocalDate targetDate, LocalDate payoutDate, Money grossAmount,
                                    Money totalFee, Money netAmount, SettlementCycle settlementCycle) {
        Status amountStatus = netAmount.isNegative() ? Status.NEGATIVE_SETTLEMENT : Status.CALCULATED;

        return new Settlement(
                "settlement-" + UUID.randomUUID().toString(),
                merchantId,
                targetDate,
                payoutDate,
                grossAmount,
                totalFee,
                netAmount,
                amountStatus,
                settlementCycle,
                0
        );
    }

    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

    public static Settlement createFailed(String merchantId, LocalDate targetDate, LocalDate payoutDate,
                                          SettlementCycle settlementCycle, Integer retryCount) {
        return new Settlement(
                "settlement-" + UUID.randomUUID().toString(),
                merchantId,
                targetDate,
                payoutDate,
                Money.ZERO,
                Money.ZERO,
                Money.ZERO,
                Status.FAILED,
                settlementCycle,
                retryCount
        );
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
