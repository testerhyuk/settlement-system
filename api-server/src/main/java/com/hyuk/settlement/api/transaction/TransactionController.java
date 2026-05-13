package com.hyuk.settlement.api.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transaction")
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> saveTransaction(@RequestBody RegisterTransactionRequest request) {
        TransactionResponse response = transactionService.saveTransaction(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
