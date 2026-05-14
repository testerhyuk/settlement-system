package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import com.hyuk.settlement.ledger.AccountConstants;
import com.hyuk.settlement.ledger.JournalEntry;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.ledger.JournalLine;
import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.ledger.enums.EntryType;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import com.hyuk.settlement.shared.SettlementDateCalculator;
import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementProcessor {
    private final SettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final SettlementDateCalculator settlementDateCalculator;
    private final MerchantRepository merchantRepository;

    @Transactional
    public void processEachSettlement(String merchantId, LocalDate targetDate) {
        List<Transaction> transactions = transactionRepository.findByMerchantIdAndSettlementDate(merchantId, targetDate);

        Money grossAmount = Money.ZERO;
        Money totalFee = Money.ZERO;

        for (Transaction transaction : transactions) {
            FeePolicy feePolicy = feePolicyRepository.findActivePolicy(merchantId, transaction.getCardCompany(), targetDate)
                    .orElseThrow(() -> new RuntimeException("수수료 정책을 찾을 수 없습니다"));

            Money fee = transaction.getAmount().times(feePolicy.getFeeRate(), transaction.getAmount().getCurrency());
            grossAmount = grossAmount.plus(transaction.getAmount());
            totalFee = totalFee.plus(fee);
        }

        Money netAmount = grossAmount.minus(totalFee);

        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다"));
        SettlementCycle settlementCycle = merchant.getSettlementCycle();

        LocalDate payoutDate = settlementDateCalculator.calculatePayoutDate(targetDate);

        Settlement settlement = Settlement.create(
                merchantId,
                targetDate,
                payoutDate,
                grossAmount,
                totalFee,
                netAmount,
                settlementCycle
        );

        settlementRepository.save(settlement);

        List<JournalLine> lines = List.of(
                JournalLine.create(AccountConstants.SETTLEMENT_PENDING, Direction.DEBIT, netAmount),
                JournalLine.create("account-payable-" + merchantId, Direction.CREDIT, netAmount)
        );

        JournalEntry journalEntry = JournalEntry.create(
                EntryType.SETTLEMENT,
                settlement.getSettlementId(),
                "정산 확정 " + merchantId,
                lines
        );

        journalEntryRepository.save(journalEntry);
    }
}
