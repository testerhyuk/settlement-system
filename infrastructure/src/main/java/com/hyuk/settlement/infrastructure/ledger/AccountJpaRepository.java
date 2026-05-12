package com.hyuk.settlement.infrastructure.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {
    List<AccountEntity> findByOwnerId(String ownerId);
}
