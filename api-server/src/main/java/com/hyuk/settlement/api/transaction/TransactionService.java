package com.hyuk.settlement.api.transaction;

import com.hyuk.settlement.shared.SettlementDateCalculator;
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
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionRepository;
import com.hyuk.settlement.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final SettlementDateCalculator settlementDateCalculator;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    public TransactionResponse saveTransaction(RegisterTransactionRequest request) {
        boolean exists = transactionRepository.existsByExternalTransactionId(request.getExternalTransactionId());

        if (exists) {
            throw new IllegalStateException("이미 존재하는 거래입니다");
        }

        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new IllegalStateException("가맹점 정보가 없습니다"));

        SettlementCycle settlementCycle = merchant.getSettlementCycle();

        LocalDate calculatedDate = settlementDateCalculator.calculate(request.getApprovedAt(), settlementCycle);

        Transaction transaction = Transaction.create(
                request.getExternalTransactionId(),
                request.getMerchantId(),
                Money.of(request.getAmount().longValue()),
                request.getTransactionType(),
                request.getCardCompany(),
                request.getApprovedAt(),
                calculatedDate
        );

        transactionRepository.save(transaction);

        FeePolicy feePolicy = feePolicyRepository.findActivePolicy(
                merchant.getMerchantId(),
                request.getCardCompany(),
                request.getApprovedAt().toLocalDate())
                .orElseThrow(() -> new IllegalStateException("수수료 정보가 없습니다"));

        Money fee = transaction.getAmount().times(feePolicy.getFeeRate(), transaction.getAmount().getCurrency());

        Money netAmount = transaction.getAmount().minus(fee);

        EntryType entryType;
        List<JournalLine> lines;

        if (request.getTransactionType() == TransactionType.PAYMENT) {
            entryType = EntryType.PAYMENT;
            lines = List.of(
                    JournalLine.create(AccountConstants.CARD_RECEIVABLE, Direction.DEBIT, transaction.getAmount()),
                    JournalLine.create("account-payable-" + request.getMerchantId(), Direction.CREDIT, netAmount),
                    JournalLine.create(AccountConstants.FEE_REVENUE, Direction.CREDIT, fee)
            );
        } else {
            entryType = request.getTransactionType() == TransactionType.CANCEL ? EntryType.CANCEL : EntryType.PARTIAL_REFUND;
            lines = List.of(
                    JournalLine.create("account-payable-" + request.getMerchantId(), Direction.DEBIT, netAmount),
                    JournalLine.create(AccountConstants.FEE_REVENUE, Direction.DEBIT, fee),
                    JournalLine.create(AccountConstants.CARD_RECEIVABLE, Direction.CREDIT, transaction.getAmount())
            );
        }

        JournalEntry journalEntry = JournalEntry.create(
                entryType,
                transaction.getTransactionId(),
                "가맹점 결제 " + transaction.getMerchantId() + " " + transaction.getAmount().getAmount() + "원",
                lines
        );

        journalEntryRepository.save(journalEntry);

        return convertToTransactionResponse(transaction);
    }

    private TransactionResponse convertToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .externalTransactionId(transaction.getExternalTransactionId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .cardCompany(transaction.getCardCompany())
                .approvedAt(transaction.getApprovedAt())
                .settlementDate(transaction.getSettlementDate())
                .build();
    }
}
