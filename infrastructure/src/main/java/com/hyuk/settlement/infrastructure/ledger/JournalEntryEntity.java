package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.JournalEntry;
import com.hyuk.settlement.ledger.enums.EntryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "journal_entry")
public class JournalEntryEntity {
    @Id
    private String entryId;
    private LocalDateTime occurredAt;
    @Enumerated(EnumType.STRING)
    private EntryType entryType;
    private String referenceId;
    private String description;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "entry_id")
    private List<JournalLineEntity> lines;

    public static JournalEntryEntity from(JournalEntry journalEntry) {
        JournalEntryEntity journalEntryEntity = new JournalEntryEntity();
        journalEntryEntity.entryId = journalEntry.getEntryId();
        journalEntryEntity.occurredAt = journalEntry.getOccurredAt();
        journalEntryEntity.entryType = journalEntry.getEntryType();
        journalEntryEntity.referenceId = journalEntry.getReferenceId();
        journalEntryEntity.description = journalEntry.getDescription();

        journalEntryEntity.lines = journalEntry.getLines().stream()
                .map(JournalLineEntity::from)
                .collect(Collectors.toList());

        return journalEntryEntity;
    }

    public JournalEntry toDomain() {
        return new JournalEntry(
                entryId,
                occurredAt,
                entryType,
                referenceId,
                description,
                lines.stream().map(JournalLineEntity::toDomain).collect(Collectors.toList())
        );
    }
}
