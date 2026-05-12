package com.hyuk.settlement.ledger;

import java.util.Optional;

public interface JournalEntryRepository {
    JournalEntry save(JournalEntry journalEntry);
    Optional<JournalEntry> findById(String id);
}
