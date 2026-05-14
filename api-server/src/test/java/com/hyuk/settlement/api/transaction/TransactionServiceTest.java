package com.hyuk.settlement.api.transaction;

import com.hyuk.settlement.feepolicy.FeePolicy;
import com.hyuk.settlement.feepolicy.FeePolicyRepository;
import com.hyuk.settlement.ledger.JournalEntry;
import com.hyuk.settlement.ledger.JournalEntryRepository;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.shared.*;
import com.hyuk.settlement.transaction.Transaction;
import com.hyuk.settlement.transaction.TransactionRepository;
import com.hyuk.settlement.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private FeePolicyRepository feePolicyRepository;
    @Mock
    private SettlementDateCalculator settlementDateCalculator;
    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private RegisterTransactionRequest request;

    @BeforeEach
    void setUp() {
        request = RegisterTransactionRequest.builder()
                .externalTransactionId("ext-01")
                .merchantId("merchant-1")
                .amount(BigDecimal.valueOf(1000))
                .cardCompany(CardCompany.SHINHAN)
                .transactionType(TransactionType.PAYMENT)
                .approvedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void externalTransactionId가_중복으로_들어오면_예외를_발생시켜야함() {
        when(transactionRepository.existsByExternalTransactionId("ext-01")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.saveTransaction(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 존재하는 거래입니다");
    }

    @Test
    void 가맹점_등록이_되어있지_않다면_예외_발생() {
        when(merchantRepository.findById(request.getMerchantId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.saveTransaction(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("가맹점 정보가 없습니다");
    }

    @Test
    void 거래_기록_정상_저장() {
        when(transactionRepository.existsByExternalTransactionId("ext-01")).thenReturn(false);
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(
                Merchant.create("123-45-67890", "카페A",
                        new BankAccount("신한", "110-111", "홍길동"),
                        SettlementCycle.D_PLUS_1)
        ));
        when(settlementDateCalculator.calculate(any(), any())).thenReturn(LocalDate.of(2026, 5, 15));
        when(feePolicyRepository.findActivePolicy(any(), any(), any())).thenReturn(Optional.of(
                FeePolicy.create("merchant-1", CardCompany.SHINHAN,
                        BigDecimal.valueOf(0.025),
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 12, 31))
        ));

        transactionService.saveTransaction(request);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    void 수수료_정책_없을때_예외_발생() {
        when(transactionRepository.existsByExternalTransactionId("ext-01")).thenReturn(false);
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(
                Merchant.create("123-45-67890", "카페A",
                        new BankAccount("신한", "110-111", "홍길동"),
                        SettlementCycle.D_PLUS_1)
        ));
        when(settlementDateCalculator.calculate(any(), any())).thenReturn(LocalDate.of(2026, 5, 15));

        when(feePolicyRepository.findActivePolicy(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.saveTransaction(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수수료 정보가 없습니다");
    }
}