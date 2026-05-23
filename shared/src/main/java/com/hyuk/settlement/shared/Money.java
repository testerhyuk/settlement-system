package com.hyuk.settlement.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private BigDecimal amount;
    private Currency currency;
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.KRW);

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) throw new RuntimeException("통화가 일치하지 않습니다");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        if (!this.currency.equals(other.currency)) throw new RuntimeException("통화가 일치하지 않습니다");
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money times(BigDecimal rate, Currency currency) {
        return new Money(this.amount.multiply(rate).setScale(currency.getScale(), RoundingMode.HALF_UP), this.currency);
    }

    @JsonIgnore
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @JsonIgnore
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }
}
