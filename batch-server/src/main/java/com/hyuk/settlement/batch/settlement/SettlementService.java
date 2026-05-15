package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.infrastructure.redis.DistributedLockManager;
import com.hyuk.settlement.shared.Currency;
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
    private final DistributedLockManager distributedLockManager;

    private final long TTL_SECONDS = 60;

    public void processSettlement(LocalDate targetDate) {
        List<String> targetMerchant = transactionRepository.findDistinctMerchantIdsBySettlementDate(targetDate);

        for (String merchantId : targetMerchant) {
            boolean result = false;

            try {
                result = distributedLockManager.tryLock("settlement:merchantId:" + merchantId, TTL_SECONDS);

                if (result) settlementProcessor.processEachSettlement(merchantId, targetDate);
            } finally {
                if(result) distributedLockManager.unlock("settlement:merchantId:" + merchantId);
            }

        }
    }
}
