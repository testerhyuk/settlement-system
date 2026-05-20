package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.infrastructure.redis.DistributedLockManager;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.settlement.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettlementRetryTest {
    @Mock
    private SettlementProcessor settlementProcessor;

    @Mock
    private DistributedLockManager distributedLockManager;

    @InjectMocks
    private SettlementService settlementService;

    @Test
    void retrySettlement_성공시_true_반환() {
        doNothing().when(settlementProcessor).processEachSettlement(any(), any());
        when(distributedLockManager.tryLock(any(), anyLong())).thenReturn(true);

        boolean result = settlementService.retrySettlement("merchant-1", LocalDate.of(2026, 5, 14));

        assertThat(result).isTrue();
    }

    @Test
    void retrySettlement_실패시_false_반환() {
        doThrow(new RuntimeException("정산 실패")).when(settlementProcessor).processEachSettlement(any(), any());
        when(distributedLockManager.tryLock(any(), anyLong())).thenReturn(true);

        boolean result = settlementService.retrySettlement("merchant-1", LocalDate.of(2026, 5, 14));

        assertThat(result).isFalse();
    }
}
