package com.hyuk.settlement.batch.payout;

import com.hyuk.settlement.infrastructure.payout.BankApiClient;
import com.hyuk.settlement.ledger.AccountConstants;
import com.hyuk.settlement.ledger.JournalEntry;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.ledger.JournalLine;
import com.hyuk.settlement.ledger.enums.Direction;
import com.hyuk.settlement.ledger.enums.EntryType;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.payout.FailureType;
import com.hyuk.settlement.payout.Payout;
import com.hyuk.settlement.payout.PayoutRepository;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import com.hyuk.settlement.shared.BankAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutProcessor {
    private final PayoutRepository payoutRepository;
    private final MerchantRepository merchantRepository;
    private final BankApiClient bankApiClient;
    private final SettlementRepository settlementRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    public void processEachPayout(Settlement s) {
        if (payoutRepository.findBySettlementId(s.getSettlementId()).isPresent()) {
            return;
        }

        BankAccount bankAccount = merchantRepository.findById(s.getMerchantId()).orElseThrow(
                () -> new IllegalArgumentException("해당 가맹점을 찾을 수 없습니다")
        ).getBankAccount();

        Payout payout = Payout.create(
                s.getSettlementId(),
                s.getMerchantId(),
                s.getNetAmount(),
                bankAccount,
                com.hyuk.settlement.payout.Status.REQUESTED
        );

        payoutRepository.save(payout);

        boolean success = false;

        for (int i = 0; i < 3; i++) {
            boolean transferResult = bankApiClient.transfer(bankAccount, s.getNetAmount());

            if (transferResult) {
                success = true;
                payout = payout.withStatus(com.hyuk.settlement.payout.Status.COMPLETED);
                s.updateStatus(Status.PAID);
                payoutRepository.save(payout);
                settlementRepository.save(s);
                recordToJournal(s, payout);
                break;
            } else {
                payout = payout.withAttemptCountPlusOne();
                payout = payout.withFailureType(FailureType.TEMPORARY);
                payoutRepository.save(payout);
            }
        }

        if (!success) {
            payout = payout.withFailureType(FailureType.PERMANENT);
            payout = payout.withStatus(com.hyuk.settlement.payout.Status.FAILED);
            payoutRepository.save(payout);
        }
    }

    private void recordToJournal(Settlement s, Payout payout) {
        List<JournalLine> lines = List.of(
                JournalLine.create(AccountConstants.MERCHANT_SENDABLE, Direction.DEBIT, s.getNetAmount()),
                JournalLine.create("account-payout-" + s.getMerchantId(), Direction.CREDIT, s.getNetAmount())
        );

        JournalEntry journalEntry = JournalEntry.create(
                EntryType.PAYOUT,
                payout.getPayoutId(),
                "가맹점 송금",
                lines
        );

        journalEntryRepository.save(journalEntry);
    }
}
