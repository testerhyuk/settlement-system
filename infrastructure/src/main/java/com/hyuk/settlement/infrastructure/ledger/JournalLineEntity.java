package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.JournalLine;
import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journal_line")
public class JournalLineEntity {
    @Id
    private String lineId;
    private String accountId;
    @Enumerated(EnumType.STRING)
    private Direction direction;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;

    public static JournalLineEntity from(JournalLine journalLine) {
        JournalLineEntity journalLineEntity = new JournalLineEntity();
        journalLineEntity.lineId = journalLine.getLineId();
        journalLineEntity.accountId = journalLine.getAccountId();
        journalLineEntity.direction = journalLine.getDirection();
        journalLineEntity.amount = journalLine.getAmount().getAmount();
        journalLineEntity.currency = journalLine.getAmount().getCurrency();
        return journalLineEntity;
    }

    public JournalLine toDomain() {
        return new JournalLine(
                lineId,
                accountId,
                direction,
                new Money(amount, currency)
        );
    }
}
