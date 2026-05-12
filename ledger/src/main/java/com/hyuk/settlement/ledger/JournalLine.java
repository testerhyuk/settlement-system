package com.hyuk.settlement.ledger;

import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.shared.Money;
import lombok.Getter;

@Getter
public class JournalLine {
    private String lineId;
    private String accountId;
    private Direction direction;
    private Money amount;

    public JournalLine(String lineId, String accountId, Direction direction, Money amount) {
        if (lineId == null || lineId.isBlank()) throw new IllegalArgumentException("lineId는 필수입니다");
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("accountId는 필수입니다");
        if (direction == null) throw new IllegalArgumentException("차변/대변 값은 필수입니다");
        if (amount == null) throw new IllegalArgumentException("금액 정보는 필수입니다");
        if (amount.isNegative()) throw new IllegalArgumentException("금액 정보는 음수값을 가질 수 없습니다");

        this.lineId = lineId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
    }
}
