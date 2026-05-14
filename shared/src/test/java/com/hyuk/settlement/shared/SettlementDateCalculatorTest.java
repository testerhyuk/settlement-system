package com.hyuk.settlement.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementDateCalculatorTest {
    private final SettlementDateCalculator settlementDateCalculator = new SettlementDateCalculator();

    @Test
    void D_PLUS_1일때_다음날이_평일인_경우_PLUS_1() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 14, 0, 0, 0);

        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.D_PLUS_1
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    void D_PLUS_2일때_2일_뒤가_평일인_경우_PLUS_2() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 13, 0, 0, 0);

        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.D_PLUS_2
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    void WEEKLY일때_다음주가_평일인_경우() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 14, 0, 0, 0);
        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.WEEKLY
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 21));
    }

    @Test
    void D_PLUS_1일때_다음날이_토요일_일때_주말을_건너뛰어야함() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 15, 0, 0, 0);
        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.D_PLUS_1
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 18));
    }

    @Test
    void D_PLUS_1일때_다음날이_일요일_일때_주말을_건너뛰어야함() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 16, 0, 0, 0);
        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.D_PLUS_1
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 18));
    }

    @Test
    void D_PLUS_2일때_2일_뒤가_주말_일때_주말을_건너뛰어야함() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 15, 0, 0, 0);
        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.D_PLUS_2
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 18));
    }

    @Test
    void WEEKLY일때_다음주가_주말일때_주말을_건너뛰어야함() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 5, 16, 0, 0, 0);
        LocalDate calculated = settlementDateCalculator.calculate(
                approvedAt,
                SettlementCycle.WEEKLY
        );

        assertThat(calculated).isEqualTo(LocalDate.of(2026, 5, 25));
    }
}