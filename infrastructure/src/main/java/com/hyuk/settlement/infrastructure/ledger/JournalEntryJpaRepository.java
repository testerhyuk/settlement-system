package com.hyuk.settlement.infrastructure.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity,String> {
}
