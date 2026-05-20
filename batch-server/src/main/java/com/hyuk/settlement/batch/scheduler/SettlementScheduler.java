package com.hyuk.settlement.batch.scheduler;

import com.hyuk.settlement.batch.payout.PayoutService;
import com.hyuk.settlement.batch.settlement.SettlementService;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {
    private final SettlementService settlementService;
    private final PayoutService payoutService;
    private final SettlementRepository settlementRepository;

    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시
    public void runSettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        settlementService.processSettlement(yesterday);
    }

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void runPayout() {
        payoutService.processPayout();
    }

    @Scheduled(cron = "0 30 1-3 * * *") // 매일 새벽 1시 30분, 2시 30분, 3시 30분
    public void retrySettlement() {
        List<Settlement> failedSettlement = settlementRepository.findByStatus(Status.FAILED);

        for (Settlement settlement : failedSettlement) {
            boolean success = false;

            if (settlement.getRetryCount() < 3) {
                settlement.incrementRetryCount();
                settlementRepository.save(settlement);

                success = settlementService.retrySettlement(settlement.getMerchantId(), settlement.getTargetDate());
            } else {
                log.error("정산 최대 재시도 초과 merchantId: {}, targetDate: {}", settlement.getMerchantId(), settlement.getTargetDate());
                settlement.updateStatus(Status.RETRY_EXHAUSTED);
                settlementRepository.save(settlement);
            }

            if (success) {
                settlement.updateStatus(Status.CALCULATED);
                settlementRepository.save(settlement);
            }
        }
    }
}
