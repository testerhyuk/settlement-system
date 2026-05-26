package com.hyuk.settlement.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, String> {
}
