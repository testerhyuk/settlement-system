package com.hyuk.settlement.batch.payout;


import com.hyuk.settlement.infrastructure.payout.BankApiClient;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.payout.FailureType;
import com.hyuk.settlement.payout.Payout;
import com.hyuk.settlement.payout.PayoutRepository;
import com.hyuk.settlement.payout.Status;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutProcessorTest {
    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private BankApiClient bankApiClient;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private PayoutProcessor payoutProcessor;

    private Settlement settlement;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        settlement = Settlement.create(
                "merchant-1",
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 15),
                Money.of(5000),
                Money.of(125),
                Money.of(4875),
                SettlementCycle.D_PLUS_1
        );

        merchant = Merchant.create(
                "business-1",
                "카페A",
                new BankAccount("신한", "110-111", "홍길동"),
                SettlementCycle.D_PLUS_1
        );
    }

    @Test
    void 송금_성공시_REQUESTED_COMPLETED_PAID로_상태변경_및_분개기록() {
        when(payoutRepository.findBySettlementId(any())).thenReturn(Optional.empty());
        when(merchantRepository.findById(any())).thenReturn(Optional.of(merchant));
        when(bankApiClient.transfer(any(), any())).thenReturn(true);

        ArgumentCaptor<Payout> payoutArgumentCaptor = ArgumentCaptor.forClass(Payout.class);
        ArgumentCaptor<Settlement> settlementArgumentCaptor = ArgumentCaptor.forClass(Settlement.class);

        payoutProcessor.processEachPayout(settlement);

        verify(payoutRepository, times(2)).save(payoutArgumentCaptor.capture());
        verify(settlementRepository, times(1)).save(settlementArgumentCaptor.capture());
        verify(payoutRepository).save(argThat(p -> p.getStatus() == Status.REQUESTED));
        verify(payoutRepository).save(argThat(p -> p.getStatus() == Status.COMPLETED));
        verify(settlementRepository).save(argThat(s -> s.getStatus() == com.hyuk.settlement.settlement.Status.PAID));
        verify(journalEntryRepository, times(1)).save(any());
    }

    @Test
    void payout이_있다면_스킵() {
        when(payoutRepository.findBySettlementId(any())).thenReturn(Optional.of(
                Payout.create(
                        "settlement-1",
                        "merchant-1",
                        Money.of(1000),
                        new BankAccount("SHINHAN", "123-456-789", "holder"),
                        Status.REQUESTED
                )
        ));

        payoutProcessor.processEachPayout(settlement);

        verify(settlementRepository, never()).save(any());
        verify(journalEntryRepository, never()).save(any());
        verify(bankApiClient, never()).transfer(any(), any());
    }

    @Test
    void 송금_3회_실패시_PERMANENT로_상태_변경() {
        when(payoutRepository.findBySettlementId(any())).thenReturn(Optional.empty());
        when(merchantRepository.findById(any())).thenReturn(Optional.of(merchant));
        when(bankApiClient.transfer(any(), any())).thenReturn(false);

        ArgumentCaptor<Payout> payoutArgumentCaptor = ArgumentCaptor.forClass(Payout.class);

        payoutProcessor.processEachPayout(settlement);

        verify(payoutRepository, times(5)).save(payoutArgumentCaptor.capture());

        assertThat(payoutArgumentCaptor.getAllValues().get(0).getFailureType()).isNull();
        assertThat(payoutArgumentCaptor.getAllValues().get(1).getFailureType()).isEqualTo(FailureType.TEMPORARY);
        assertThat(payoutArgumentCaptor.getAllValues().get(2).getFailureType()).isEqualTo(FailureType.TEMPORARY);
        assertThat(payoutArgumentCaptor.getAllValues().get(3).getFailureType()).isEqualTo(FailureType.TEMPORARY);
        assertThat(payoutArgumentCaptor.getAllValues().get(4).getFailureType()).isEqualTo(FailureType.PERMANENT);

        assertThat(payoutArgumentCaptor.getAllValues().get(4).getStatus()).isEqualTo(Status.FAILED);
    }

    @Test
    void 가맹점_정보가_없다면_예외_발생() {
        when(payoutRepository.findBySettlementId(any())).thenReturn(Optional.empty());
        when(merchantRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payoutProcessor.processEachPayout(settlement))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 가맹점을 찾을 수 없습니다");
    }
}