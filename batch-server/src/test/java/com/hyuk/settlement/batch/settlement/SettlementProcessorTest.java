package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
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
    private List<Transaction> cancelTransactions;
    private List<Transaction> partialRefundTransactions;
    private List<Transaction> minusTransactions;
    private Settlement negativeSettlement;

    @BeforeEach
    void setUp() {
        transactions = List.of(
                Transaction.create("ext-01", "merchant-1", Money.of(5000),
                        TransactionType.PAYMENT, CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14))
        );

        cancelTransactions = List.of(
            Transaction.create("ext-02", "merchant-2", Money.of(5000),
                    TransactionType.PAYMENT, CardCompany.SHINHAN,
                    LocalDateTime.now(), LocalDate.of(2026, 5, 14)),
            Transaction.createCancelOrRefund("ext-03", "ext-02",
          "merchant-2", Money.of(5000), TransactionType.CANCEL,CardCompany.SHINHAN,
                    LocalDateTime.now(), LocalDate.of(2026, 5, 14))
        );

        partialRefundTransactions = List.of(
                Transaction.create("ext-04", "merchant-4", Money.of(5000),
                        TransactionType.PAYMENT, CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14)),
                Transaction.createCancelOrRefund("ext-05", "ext-04",
                        "merchant-4", Money.of(2000), TransactionType.PARTIAL_REFUND,CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14))
        );

        minusTransactions = List.of(
                Transaction.create("ext-04", "merchant-4", Money.of(5000),
                        TransactionType.PAYMENT, CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14)),
                Transaction.createCancelOrRefund("ext-05", "ext-04",
                        "merchant-4", Money.of(6000), TransactionType.PARTIAL_REFUND,CardCompany.SHINHAN,
                        LocalDateTime.now(), LocalDate.of(2026, 5, 14))
        );

        negativeSettlement = Settlement.create(
                "merchant-5",
                LocalDate.of(2026, 5, 13),
                LocalDate.of(2026, 5, 14),
                Money.of(-1000),
                Money.of(0),
                Money.of(-1000),
                SettlementCycle.D_PLUS_1
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

        when(settlementRepository.findByMerchantIdAndStatus(any(), any())).thenReturn(List.of());

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

    @Test
    void 결제_취소_후_grossAmount가_0인지_확인() {
        when(transactionRepository.findByMerchantIdAndSettlementDate("merchant-2", LocalDate.of(2026, 5, 14)))
                .thenReturn(cancelTransactions);

        when(feePolicyRepository.findActivePolicy(any(), any(), any()))
                .thenReturn(Optional.of(FeePolicy.create(
                        "merchant-2",
                        CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))));

        when(settlementRepository.findByMerchantIdAndStatus(any(), any())).thenReturn(List.of());

        when(merchantRepository.findById("merchant-2")).thenReturn(Optional.of(
                Merchant.create(
                        "business-2",
                        "name",
                        new BankAccount("SHINHAN","123-456-788", "hyuk"),
                        SettlementCycle.D_PLUS_1
                )
        ));

        when(settlementDateCalculator.calculatePayoutDate(any())).thenReturn(LocalDate.of(2026, 5, 15));

        settlementProcessor.processEachSettlement("merchant-2", LocalDate.of(2026, 5, 14));

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementRepository, times(1)).save(captor.capture());
        verify(journalEntryRepository, times(1)).save(any());

        Settlement settlement = captor.getValue();

        assertThat(settlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(settlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(settlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void 부분_환불_후_해당_금액만큼_차감_확인() {
        when(transactionRepository.findByMerchantIdAndSettlementDate("merchant-4", LocalDate.of(2026, 5, 14)))
                .thenReturn(partialRefundTransactions);

        when(feePolicyRepository.findActivePolicy(any(), any(), any()))
                .thenReturn(Optional.of(FeePolicy.create(
                        "merchant-4",
                        CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))));

        when(settlementRepository.findByMerchantIdAndStatus(any(), any())).thenReturn(List.of());

        when(merchantRepository.findById("merchant-4")).thenReturn(Optional.of(
                Merchant.create(
                        "business-4",
                        "name",
                        new BankAccount("SHINHAN","123-456-788", "hyuk"),
                        SettlementCycle.D_PLUS_1
                )
        ));

        when(settlementDateCalculator.calculatePayoutDate(any())).thenReturn(LocalDate.of(2026, 5, 15));

        settlementProcessor.processEachSettlement("merchant-4", LocalDate.of(2026, 5, 14));

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementRepository, times(1)).save(captor.capture());
        verify(journalEntryRepository, times(1)).save(any());

        Settlement settlement = captor.getValue();

        assertThat(settlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(settlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(settlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2925));
    }

    @Test
    void 환불이_결제보다_많을때_NEGATIVE_SEETLEMENT_상태로_저장() {
        when(transactionRepository.findByMerchantIdAndSettlementDate("merchant-4", LocalDate.of(2026, 5, 14)))
                .thenReturn(minusTransactions);
        when(feePolicyRepository.findActivePolicy(any(), any(), any()))
                .thenReturn(Optional.of(FeePolicy.create(
                        "merchant-4",
                        CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))));

        when(settlementRepository.findByMerchantIdAndStatus(any(), any())).thenReturn(List.of());

        when(merchantRepository.findById("merchant-4")).thenReturn(Optional.of(
                Merchant.create(
                        "business-4",
                        "name",
                        new BankAccount("SHINHAN","123-456-788", "hyuk"),
                        SettlementCycle.D_PLUS_1
                )
        ));

        when(settlementDateCalculator.calculatePayoutDate(any())).thenReturn(LocalDate.of(2026, 5, 15));

        settlementProcessor.processEachSettlement("merchant-4", LocalDate.of(2026, 5, 14));

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementRepository, times(1)).save(captor.capture());
        verify(journalEntryRepository, times(1)).save(any());

        Settlement settlement = captor.getValue();

        assertThat(settlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-1000));
        assertThat(settlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-25));
        assertThat(settlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-975));

        assertThat(settlement.getStatus()).isEqualTo(Status.NEGATIVE_SETTLEMENT);
    }

    @Test
    void NEGATIVE_SETTLEMENT가_있을때_다음_정산에서_차감되고_RECOVERED로_변경() {
        when(transactionRepository.findByMerchantIdAndSettlementDate("merchant-1", LocalDate.of(2026, 5, 14)))
                .thenReturn(transactions);

        when(feePolicyRepository.findActivePolicy(any(), any(), any()))
                .thenReturn(Optional.of(FeePolicy.create(
                        "merchant-1",
                        CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))));

        when(settlementRepository.findByMerchantIdAndStatus(any(), any())).thenReturn(List.of(negativeSettlement));

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

        verify(settlementRepository, times(2)).save(captor.capture());
        verify(journalEntryRepository, times(2)).save(any());

        List<Settlement> savedSettlements = captor.getAllValues();
        Settlement recoveredSettlement = savedSettlements.get(0); // RECOVERED
        Settlement newSettlement = savedSettlements.get(1); // CALCULATED

        assertThat(recoveredSettlement.getStatus()).isEqualTo(Status.RECOVERED);
        assertThat(newSettlement.getGrossAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(newSettlement.getTotalFee().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(125));
        assertThat(newSettlement.getNetAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3875));
    }
}