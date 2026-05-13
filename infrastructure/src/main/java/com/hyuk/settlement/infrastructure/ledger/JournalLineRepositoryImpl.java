package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.JournalLineRepository;
import com.hyuk.settlement.ledger.enums.Direction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class JournalLineRepositoryImpl implements JournalLineRepository {
    private final JournalLineJpaRepository journalLineJpaRepository;

    @Override
    public BigDecimal sumByAccountIdAndDirection(String accountId, Direction direction) {
        return journalLineJpaRepository.sumByAccountIdAndDirection(accountId, direction);
    }
}
