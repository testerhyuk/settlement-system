package com.hyuk.settlement.api.ledger;

import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    @GetMapping("/{accountId}/{currency}")
    public ResponseEntity<Money> getBalance(@PathVariable("accountId") String accountId, @RequestParam(defaultValue = "KRW") String currency) {
        Currency curr = Currency.valueOf(currency);

        Money res = ledgerService.getBalance(accountId, curr);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
