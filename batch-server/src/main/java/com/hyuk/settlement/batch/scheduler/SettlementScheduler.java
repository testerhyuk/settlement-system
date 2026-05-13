package com.hyuk.settlement.batch.scheduler;

import com.hyuk.settlement.batch.payout.PayoutService;
import com.hyuk.settlement.batch.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {
    private final SettlementService settlementService;
    private final PayoutService payoutService;

    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    public void runSettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        settlementService.processSettlement(yesterday);
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void runPayout() {
        payoutService.processPayout();
    }
}
