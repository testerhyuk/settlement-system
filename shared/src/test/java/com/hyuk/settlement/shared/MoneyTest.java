package com.hyuk.settlement.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
    @Test
    void 같은_통화끼리_합산() {
        Money money1 = Money.of(5000);
        Money money2 = Money.of(1000);

        Money result = money1.plus(money2);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(6000));
    }

    @Test
    void 다른_통화끼리_더하면_예외발생() {
        Money money1 = Money.of(5000);

        assertThatThrownBy(() -> money1.plus(new Money(BigDecimal.valueOf(1000), Currency.USD))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void 같은_통화끼리_빼기() {
        Money money1 = Money.of(5000);
        Money money2 = Money.of(1000);
        Money result= money1.minus(money2);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(4000));
    }

    @Test
    void 다른_통화끼리_빼면_예외발생() {
        Money money1 = Money.of(5000);

        assertThatThrownBy(() -> money1.minus(new Money(BigDecimal.valueOf(1000), Currency.USD))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void 같은_통화끼리_곱하기() {
        Money money1 = Money.of(5000);
        BigDecimal feeRate = BigDecimal.valueOf(0.025);
        Money result = money1.times(feeRate, Currency.USD);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));
    }

    @Test
    void 음수_체크() {
        Money money1 = Money.of(-5000);
        Money money2 = Money.of(1000);

        assertThat(money1.isNegative()).isTrue();
        assertThat(money2.isNegative()).isFalse();
    }

    @Test
    void 소수점_반올림_체크() {
        Money money = Money.of(7777);
        BigDecimal feeRate = BigDecimal.valueOf(0.025);

        assertThat(money.times(feeRate, Currency.USD).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(194.43));
    }
}