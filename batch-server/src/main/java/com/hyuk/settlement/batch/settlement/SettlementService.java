package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final TransactionRepository transactionRepository;
    private final SettlementProcessor settlementProcessor;

    public void processSettlement(LocalDate targetDate) {
        List<String> targetMerchant = transactionRepository.findDistinctMerchantIdsBySettlementDate(targetDate);

        for (String merchantId : targetMerchant) {
            settlementProcessor.processEachSettlement(merchantId, targetDate);
        }
    }
}
