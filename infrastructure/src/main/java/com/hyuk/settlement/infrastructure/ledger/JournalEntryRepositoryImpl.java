package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.JournalEntry;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JournalEntryRepositoryImpl implements JournalEntryRepository {
    private final JournalEntryJpaRepository journalEntryJpaRepository;

    @Override
    public JournalEntry save(JournalEntry journalEntry) {
        JournalEntryEntity journalEntryEntity = JournalEntryEntity.from(journalEntry);
        journalEntryJpaRepository.save(journalEntryEntity);
        return journalEntryEntity.toDomain();
    }

    @Override
    public Optional<JournalEntry> findById(String id) {
        return journalEntryJpaRepository.findById(id).map(JournalEntryEntity::toDomain);
    }
}
