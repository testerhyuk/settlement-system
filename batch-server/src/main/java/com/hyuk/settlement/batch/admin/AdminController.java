package com.hyuk.settlement.batch.admin;

import com.hyuk.settlement.batch.payout.PayoutService;
import com.hyuk.settlement.batch.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminController {
    private final SettlementService settlementService;
    private final PayoutService payoutService;

    @PostMapping("/settlement/trigger")
    public void settlementTrigger(@RequestParam LocalDate targetDate) {
        settlementService.processSettlement(targetDate);
    }

    @PostMapping("/payout/trigger")
    public void payoutTrigger() {
        payoutService.processPayout();
    }
}
