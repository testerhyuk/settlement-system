package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.infrastructure.redis.DistributedLockManager;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.merchant.MerchantStatus;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    private final TransactionRepository transactionRepository;
    private final SettlementProcessor settlementProcessor;
    private final DistributedLockManager distributedLockManager;
    private final MerchantRepository merchantRepository;

    private final long TTL_SECONDS = 60;

    public void processSettlement(LocalDate targetDate) {
        List<String> targetMerchant = transactionRepository.findDistinctMerchantIdsBySettlementDate(targetDate);

        for (String merchantId : targetMerchant) {
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다"));

            if (merchant.getMerchantStatus() == MerchantStatus.SUSPENDED || merchant.getMerchantStatus() == MerchantStatus.INACTIVE) {
                log.info("정산 스킵 - merchantId: {}, status: {}", merchantId, merchant.getMerchantStatus());
                continue;
            }

            boolean result = false;

            try {
                result = distributedLockManager.tryLock("settlement:merchantId:" + merchantId, TTL_SECONDS);

                if (result) settlementProcessor.processEachSettlement(merchantId, targetDate);
            } finally {
                if(result) distributedLockManager.unlock("settlement:merchantId:" + merchantId);
            }
        }
    }

    // 정산 실패 재시도
    public boolean retrySettlement(String merchantId, LocalDate targetDate) {
        boolean result = false;
        boolean success = false;

        try {
            result = distributedLockManager.tryLock("settlement:merchantId:" + merchantId, TTL_SECONDS);

            if (result) settlementProcessor.processEachSettlement(merchantId, targetDate);

            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (result) distributedLockManager.unlock("settlement:merchantId:" + merchantId);
        }
    }
}
