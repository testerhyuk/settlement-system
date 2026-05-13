package com.hyuk.settlement.batch.payout;

import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutService {
    private final SettlementRepository settlementRepository;
    private final PayoutProcessor payoutProcessor;

    public void processPayout() {
        List<Settlement> settlement = settlementRepository.findByStatus(Status.CALCULATED);

        for (Settlement s : settlement) {
            payoutProcessor.processEachPayout(s);
        }
    }
}
