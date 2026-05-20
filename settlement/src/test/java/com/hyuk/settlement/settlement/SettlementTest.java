package com.hyuk.settlement.settlement;

import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {
    @Test
    void netAmount의_값은_grossAmount에서_totalFee를_뺀_값이면_정상_생성() {
        Settlement settlement = Settlement.create(
                "merchant-1",
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 15),
                Money.of(5000),
                Money.of(100),
                Money.of(4900),
                SettlementCycle.D_PLUS_1
        );

        assertThat(settlement.getMerchantId()).isEqualTo("merchant-1");
        assertThat(settlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(settlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(settlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(4900));
        assertThat(settlement.getStatus()).isEqualTo(Status.CALCULATED);
    }
}