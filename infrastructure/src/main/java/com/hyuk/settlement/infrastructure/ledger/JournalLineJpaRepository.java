package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.enums.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface JournalLineJpaRepository extends JpaRepository<JournalLineEntity, String> {
    @Query("SELECT SUM(j.amount) FROM JournalLineEntity j WHERE j.accountId = :accountId AND j.direction = :direction")
    BigDecimal sumByAccountIdAndDirection(@Param("accountId") String accountId, @Param("direction") Direction direction);
}
