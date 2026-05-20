package com.hyuk.settlement.batch.settlement;

import com.hyuk.settlement.infrastructure.redis.DistributedLockManager;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.merchant.MerchantStatus;
import com.hyuk.settlement.settlement.SettlementRepository;
import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.SettlementCycle;
import com.hyuk.settlement.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettlementServiceTest {
    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DistributedLockManager distributedLockManager;

    @InjectMocks
    private SettlementService settlementService;

    List<String> targetMerchant;
    Merchant merchant1;
    Merchant merchant2;

    @BeforeEach
    void setUp() {
        targetMerchant = List.of(
                "merchant1",
                "merchant2"
        );

        merchant1 = new Merchant(
                "merchant-1",
                "business-1",
                "cafe",
                new BankAccount("SHINHAN", "123-456-789", "hyuk"),
                SettlementCycle.D_PLUS_1,
                MerchantStatus.SUSPENDED
        );

        merchant2 = new Merchant(
                "merchant-2",
                "business-2",
                "cafe2",
                new BankAccount("SHINHAN", "113-456-789", "hyuk2"),
                SettlementCycle.D_PLUS_1,
                MerchantStatus.INACTIVE
        );
    }

    @Test
    void merchantStatus가_SUSPENDED_또는_INACTIVE_일때_continue() {
        when(transactionRepository.findDistinctMerchantIdsBySettlementDate(any())).thenReturn(targetMerchant);

        when(merchantRepository.findById(any()))
                .thenReturn(Optional.of(merchant1))
                .thenReturn(Optional.of(merchant2));

        settlementService.processSettlement(LocalDate.of(2026, 5, 20));

        verify(merchantRepository, times(2)).findById(any());
        verify(distributedLockManager, never()).tryLock(any(), anyLong());
    }
}
