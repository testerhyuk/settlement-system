package com.hyuk.settlement.ledger;

import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.ledger.enums.EntryType;
import com.hyuk.settlement.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {
    @Test
    void 분개기록_생성할때_차변의_합과_대변의_합이_일치시_정상생성() {
        List<JournalLine> lines = List.of(
                JournalLine.create("account-1", Direction.DEBIT, Money.of(1000)),
                JournalLine.create("account-2", Direction.CREDIT, Money.of(1000))
        );

        JournalEntry entry = JournalEntry.create(
                EntryType.PAYMENT,
                "reference-1",
                "description",
                lines
        );

        assertThat(entry.getEntryType()).isEqualTo(EntryType.PAYMENT);
        assertThat(entry.getReferenceId()).isEqualTo("reference-1");
        assertThat(entry.getDescription()).isEqualTo("description");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(entry.getEntryId()).isNotNull();
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    void 분개기록_생성할때_차변의_합과_대변의_합이_다를시_예외발생() {
        List<JournalLine> lines = List.of(
                JournalLine.create("account-1", Direction.DEBIT, Money.of(1000)),
                JournalLine.create("account-2", Direction.CREDIT, Money.of(2000))
        );

        assertThatThrownBy(() -> JournalEntry.create(
                EntryType.PAYMENT,
                "reference-1",
                "description",
                lines
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("차변과 대변의 합은 항상 일치해야 합니다");
    }

    @Test
    void 분개라인이_비어있을때_예외발생() {
        List<JournalLine> lines = List.of();

        assertThatThrownBy(() -> JournalEntry.create(
                EntryType.PAYMENT,
                "reference-1",
                "description",
                lines
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분개 라인 정보는 필수입니다");
    }
}