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
import com.hyuk.settlement.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
    @Retryable(maxAttempts = 3)
    public void processEachSettlement(String merchantId, LocalDate targetDate) {
        List<Transaction> transactions = transactionRepository.findByMerchantIdAndSettlementDate(merchantId, targetDate);

        TransactionAmounts amounts = calculateAmounts(transactions, merchantId, targetDate);

        Money netAmount = amounts.grossAmount.minus(amounts.totalFee);

        netAmount = applyNegativeSettlements(merchantId, netAmount);

        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다"));
        SettlementCycle settlementCycle = merchant.getSettlementCycle();

        LocalDate payoutDate = settlementDateCalculator.calculatePayoutDate(targetDate);

        Settlement settlement = Settlement.create(
                merchantId,
                targetDate,
                payoutDate,
                amounts.grossAmount,
                amounts.totalFee,
                netAmount,
                settlementCycle
        );

        settlementRepository.save(settlement);

        saveSettlementJournalEntry(settlement, merchantId, netAmount);
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverSettlement(Exception e, String merchantId, LocalDate targetDate) {
        boolean exists = settlementRepository.existsByMerchantIdAndStatusAndTargetDate(
                merchantId, Status.FAILED, targetDate);

        if (!exists) {
            LocalDate payoutDate = settlementDateCalculator.calculatePayoutDate(targetDate);
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new IllegalArgumentException("가맹점을 찾을 수 없습니다"));

            Settlement settlement = Settlement.createFailed(merchantId, targetDate, payoutDate, merchant.getSettlementCycle(), 0);
            settlementRepository.save(settlement);
        }
    }

    // 1. 거래 집계
    private record TransactionAmounts(Money grossAmount, Money totalFee) {}

    private TransactionAmounts calculateAmounts(List<Transaction> transactions, String merchantId, LocalDate targetDate) {
        Money grossAmount = Money.ZERO;
        Money totalFee = Money.ZERO;

        for (Transaction transaction : transactions) {
            FeePolicy feePolicy = feePolicyRepository.findActivePolicy(merchantId, transaction.getCardCompany(), targetDate)
                    .orElseThrow(() -> new RuntimeException("수수료 정책을 찾을 수 없습니다"));

            Money fee = transaction.getAmount().times(feePolicy.getFeeRate(), transaction.getAmount().getCurrency());

            if (transaction.getTransactionType() == TransactionType.PAYMENT) {
                grossAmount = grossAmount.plus(transaction.getAmount());
                totalFee = totalFee.plus(fee);
            } else if (transaction.getTransactionType() == TransactionType.CANCEL || transaction.getTransactionType() == TransactionType.PARTIAL_REFUND) {
                grossAmount = grossAmount.minus(transaction.getAmount());
                totalFee = totalFee.minus(fee);
            }
        }

        return new TransactionAmounts(grossAmount, totalFee);
    }

    // 2. 마이너스 정산 차감
    private Money applyNegativeSettlements(String merchantId, Money netAmount) {
        List<Settlement> negativeSettlements = settlementRepository.findByMerchantIdAndStatus(merchantId, Status.NEGATIVE_SETTLEMENT);

        for (Settlement negative : negativeSettlements) {
            netAmount = netAmount.plus(negative.getNetAmount());
            negative.updateStatus(Status.RECOVERED);
            settlementRepository.save(negative);

            List<JournalLine> negativeLines = List.of(
                    JournalLine.create("account-payable-" + merchantId, Direction.DEBIT, negative.getNetAmount().negate()),
                    JournalLine.create(AccountConstants.SETTLEMENT_PENDING, Direction.CREDIT, negative.getNetAmount().negate())
            );

            JournalEntry negativeEntry = JournalEntry.create(
                    EntryType.NEGATIVE_SETTLEMENT_DEDUCTION,
                    negative.getSettlementId(),
                    "마이너스 정산 차감 " + merchantId,
                    negativeLines
            );

            journalEntryRepository.save(negativeEntry);
        }

        return netAmount;
    }

    // 3. 분개 기록
    private void saveSettlementJournalEntry(Settlement settlement, String merchantId, Money netAmount) {
        Money absAmount = netAmount.isNegative() ? netAmount.negate() : netAmount;

        List<JournalLine> lines;
        if (netAmount.isNegative()) {
            lines = List.of(
                    JournalLine.create("account-payable-" + merchantId, Direction.DEBIT, absAmount),
                    JournalLine.create(AccountConstants.SETTLEMENT_PENDING, Direction.CREDIT, absAmount)
            );
        } else {
            lines = List.of(
                    JournalLine.create(AccountConstants.SETTLEMENT_PENDING, Direction.DEBIT, absAmount),
                    JournalLine.create("account-payable-" + merchantId, Direction.CREDIT, absAmount)
            );
        }

        JournalEntry journalEntry = JournalEntry.create(
                EntryType.SETTLEMENT,
                settlement.getSettlementId(),
                "정산 확정 " + merchantId,
                lines
        );

        journalEntryRepository.save(journalEntry);
    }
}
