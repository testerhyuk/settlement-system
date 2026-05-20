package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.batch.payout.PayoutService;
import com.hyuk.settlement.batch.scheduler.SettlementScheduler;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.settlement.Status;
import com.hyuk.settlement.shared.Currency;
import com.hyuk.settlement.shared.Money;
import com.hyuk.settlement.shared.SettlementCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettlementSchedulerTest {
    @Mock
    private SettlementService settlementService;

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    @Mock
    private PayoutService payoutService;

    List<Settlement> failedSettlement;
    List<Settlement> exhaustedSettlement;

    @BeforeEach
    void setUp() {
        failedSettlement = List.of(
                new Settlement(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 5, 20),
                        LocalDate.of(2026, 5, 21),
                        new Money(BigDecimal.valueOf(2000), Currency.KRW),
                        new Money(BigDecimal.valueOf(50), Currency.KRW),
                        new Money(BigDecimal.valueOf(1950), Currency.KRW),
                        Status.FAILED,
                        SettlementCycle.D_PLUS_1,
                        0
                )
        );

        exhaustedSettlement = List.of(
                new Settlement(
                        "settlement-1",
                        "merchant-1",
                        LocalDate.of(2026, 5, 20),
                        LocalDate.of(2026, 5, 21),
                        new Money(BigDecimal.valueOf(2000), Currency.KRW),
                        new Money(BigDecimal.valueOf(50), Currency.KRW),
                        new Money(BigDecimal.valueOf(1950), Currency.KRW),
                        Status.FAILED,
                        SettlementCycle.D_PLUS_1,
                        3
                )
        );
    }

    @Test
    void 재시도_성공시_CALCULATED로_상태_변경() {
        when(settlementRepository.findByStatus(any())).thenReturn(failedSettlement);

        when(settlementService.retrySettlement(any(), any())).thenReturn(true);

        settlementScheduler.retrySettlement();

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementService, times(1)).retrySettlement(any(), any());
        verify(settlementRepository, times(2)).save(captor.capture());

        List<Settlement> saved = captor.getAllValues();

        assertThat(saved.get(1).getStatus()).isEqualTo(Status.CALCULATED);
        assertThat(saved.get(1).getRetryCount()).isEqualTo(1);
    }

    @Test
    void 재시도_3회_이상일_경우_RETRY_EXHAUSTED로_상태_변경() {
        when(settlementRepository.findByStatus(any())).thenReturn(exhaustedSettlement);

        settlementScheduler.retrySettlement();

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        verify(settlementRepository, times(1)).save(captor.capture());

        List<Settlement> saved = captor.getAllValues();

        assertThat(saved.get(0).getStatus()).isEqualTo(Status.RETRY_EXHAUSTED);
    }
}
