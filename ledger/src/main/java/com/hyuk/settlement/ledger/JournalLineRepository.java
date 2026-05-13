package com.hyuk.settlement.ledger;

import com.hyuk.settlement.ledger.enums.Direction;

import java.math.BigDecimal;

public interface JournalLineRepository {
    BigDecimal sumByAccountIdAndDirection(String accountId, Direction direction);
}