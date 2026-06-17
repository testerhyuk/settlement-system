package com.hyuk.settlement.infrastructure.dlq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DlqMessageRepository extends JpaRepository<DlqMessageEntity, String> {
    List<DlqMessageEntity> findByDlqStatus(DlqStatus status);
}
