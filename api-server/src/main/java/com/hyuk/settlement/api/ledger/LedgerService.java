package com.hyuk.settlement.api.ledger;

import com.hyuk.settlement.ledger.JournalLineRepository;
import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final JournalLineRepository journalLineJpaRepository;

    public Money getBalance(String accountId, Currency currency) {
        BigDecimal debit = journalLineJpaRepository.sumByAccountIdAndDirection(accountId, Direction.DEBIT);
        BigDecimal credit = journalLineJpaRepository.sumByAccountIdAndDirection(accountId, Direction.CREDIT);

        debit = debit != null ? debit : BigDecimal.ZERO;
        credit = credit != null ? credit : BigDecimal.ZERO;

        BigDecimal res = debit.subtract(credit);

        return new Money(res, currency);
    }
}
