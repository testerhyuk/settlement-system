package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.shared.*;
import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionRepository;
import com.hyuk.settlement.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementProcessorTest {
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FeePolicyRepository feePolicyRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private SettlementDateCalculator settlementDateCalculator;
    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private SettlementProcessor settlementProcessor;

    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        transactions = List.of(
                Transaction.create("ext-01", "merchant-1", Money.of(5000),
                        TransactionType.PAYMENT, CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14))
        );
    }

    @Test
    void 정산_금액_정확성_확인() {
        when(transactionRepository.findByMerchantIdAndSettlementDate("merchant-1", LocalDate.of(2026, 5, 14)))
                .thenReturn(transactions);

        when(feePolicyRepository.findActivePolicy(any(), any(), any()))
                .thenReturn(Optional.of(FeePolicy.create(
                        "merchant-1",
                        CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))));

        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(
                Merchant.create(
                        "business-1",
                        "name",
                        new BankAccount("SHINHAN","123-456-789", "hyuk"),
                        SettlementCycle.D_PLUS_1
                )
        ));

        when(settlementDateCalculator.calculatePayoutDate(any())).thenReturn(LocalDate.of(2026, 5, 15));

        settlementProcessor.processEachSettlement("merchant-1", LocalDate.of(2026, 5, 14));

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementRepository, times(1)).save(captor.capture());
        verify(journalEntryRepository, times(1)).save(any());

        Settlement settlement = captor.getValue();

        assertThat(settlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(settlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));
        assertThat(settlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(4875));
    }
}