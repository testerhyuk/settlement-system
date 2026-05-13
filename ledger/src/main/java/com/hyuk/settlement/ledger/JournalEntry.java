package com.hyuk.settlement.ledger;

import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.ledger.enums.EntryType;
import com.hyuk.settlement.shared.Money;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class JournalEntry {
    private String entryId;
    private LocalDateTime occurredAt;
    private EntryType entryType;
    private String referenceId;
    private String description;
    private List<JournalLine> lines;

    public JournalEntry(String entryId, LocalDateTime occurredAt, EntryType entryType, String referenceId, String description, List<JournalLine> lines) {
        if (entryId == null || entryId.isBlank()) throw new IllegalArgumentException("entryId는 필수입니다");
        if (entryType == null) throw new IllegalArgumentException("분개 유형은 필수입니다");
        if (referenceId == null || referenceId.isBlank()) throw new IllegalArgumentException("referenceId는 필수입니다");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("설명 값은 필수입니다");
        if (occurredAt == null) throw new IllegalArgumentException("발생일은 필수입니다");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("분개 라인 정보는 필수입니다");

        Money sumDebit = lines.stream().filter(line -> line.getDirection().equals(Direction.DEBIT))
                .map(JournalLine::getAmount)
                .reduce(Money.ZERO, Money::plus);

        Money sumCredit = lines.stream().filter(line -> line.getDirection().equals(Direction.CREDIT))
                .map(JournalLine::getAmount)
                .reduce(Money.ZERO, Money::plus);

        if (sumDebit.getAmount().compareTo(sumCredit.getAmount()) != 0) throw new IllegalArgumentException("차변과 대변의 합은 항상 일치해야 합니다");

        this.entryId = entryId;
        this.occurredAt = occurredAt;
        this.entryType = entryType;
        this.referenceId = referenceId;
        this.description = description;
        this.lines = lines;
    }

    public static JournalEntry create(EntryType entryType, String referenceId, String description, List<JournalLine> lines) {
        return new JournalEntry(
                "journal-" + UUID.randomUUID().toString(),
                LocalDateTime.now(),
                entryType,
                referenceId,
                description,
                lines
        );
    }
}
