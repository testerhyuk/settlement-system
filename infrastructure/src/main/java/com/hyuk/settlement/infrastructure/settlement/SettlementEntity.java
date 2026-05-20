package com.hyuk.settlement.infrastructure.settlement;

import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.Status;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement", uniqueConstraints = @UniqueConstraint(
        columnNames = {"merchantId", "target_date"}
))
public class SettlementEntity {
    @Id
    private String settlementId;
    private String merchantId;
    private LocalDate targetDate;
    private LocalDate payoutDate;
    private BigDecimal grossAmount;
    private BigDecimal totalFee;
    private BigDecimal netAmount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Enumerated(EnumType.STRING)
    private SettlementCycle settlementCycle;
    private Integer retryCount;

    public static SettlementEntity from(Settlement settlement) {
        SettlementEntity settlementEntity = new SettlementEntity();
        settlementEntity.settlementId = settlement.getSettlementId();
        settlementEntity.merchantId = settlement.getMerchantId();
        settlementEntity.targetDate = settlement.getTargetDate();
        settlementEntity.payoutDate = settlement.getPayoutDate();
        settlementEntity.grossAmount = settlement.getGrossAmount().getAmount();
        settlementEntity.totalFee = settlement.getTotalFee().getAmount();
        settlementEntity.netAmount = settlement.getNetAmount().getAmount();
        settlementEntity.currency = settlement.getGrossAmount().getCurrency();
        settlementEntity.status = settlement.getStatus();
        settlementEntity.settlementCycle = settlement.getSettlementCycle();
        settlementEntity.retryCount = settlement.getRetryCount();
        return settlementEntity;
    }

    public Settlement toDomain() {
        return new Settlement(
                settlementId,
                merchantId,
                targetDate,
                payoutDate,
                new Money(grossAmount, currency),
                new Money(totalFee, currency),
                new Money(netAmount, currency),
                status,
                settlementCycle,
                retryCount
        );
    }
}
