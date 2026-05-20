package com.hyuk.settlement.batch.admin;

import com.hyuk.settlement.batch.payout.PayoutService;
import com.hyuk.settlement.batch.settlement.SettlementService;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.merchant.MerchantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminController {
    private final SettlementService settlementService;
    private final PayoutService payoutService;
    private final MerchantRepository merchantRepository;

    @PostMapping("/settlement/trigger")
    public void settlementTrigger(@RequestParam LocalDate targetDate) {
        settlementService.processSettlement(targetDate);
    }

    @PostMapping("/payout/trigger")
    public void payoutTrigger() {
        payoutService.processPayout();
    }

    @PostMapping("/settlement/suspend/{merchantId}")
    public void settlementSuspend(@PathVariable String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new IllegalStateException("가맹점을 찾을 수 없습니다"));

        merchant.changeStatus(MerchantStatus.SUSPENDED);

        merchantRepository.save(merchant);
    }

    @PostMapping("/settlement/resume/{merchantId}")
    public void settlementActive(@PathVariable String merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new IllegalStateException("가맹점을 찾을 수 없습니다"));

        merchant.changeStatus(MerchantStatus.ACTIVE);

        merchantRepository.save(merchant);
    }
}
