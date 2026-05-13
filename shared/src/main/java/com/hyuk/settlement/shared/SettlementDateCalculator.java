package com.hyuk.settlement.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SettlementDateCalculator {
    public LocalDate calculate(LocalDateTime approvedAt, SettlementCycle settlementCycle) {
        LocalDate date = approvedAt
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate();

        if (settlementCycle.equals(SettlementCycle.D_PLUS_1)) {
            LocalDate nextDay = date.plusDays(1);
            return skipWeekend(nextDay);
        } else if (settlementCycle.equals(SettlementCycle.D_PLUS_2)) {
            LocalDate nextDay = date.plusDays(2);
            return skipWeekend(nextDay);
        } else if (settlementCycle.equals(SettlementCycle.WEEKLY)) {
            LocalDate nextWeek = date.plusWeeks(1);
            return skipWeekend(nextWeek);
        }

        throw new IllegalArgumentException("정산 주기가 잘못되었습니다");
    }

    public LocalDate calculatePayoutDate(LocalDate targetDate) {
        LocalDate nextDay = targetDate.plusDays(1);

        if (nextDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return nextDay.plusDays(2);
        } else if (nextDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return nextDay.plusDays(1);
        }

        return nextDay;
    }

    private LocalDate skipWeekend(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return date.plusDays(2);
        } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }

        return date;
    }
}
