package com.hyuk.settlement.api.transaction;

import com.hyuk.settlement.api.merchant.ChangeBankAccountRequest;
import com.hyuk.settlement.api.merchant.MerchantService;
import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.merchant.MerchantRepository;
import com.hyuk.settlement.settlement.Settlement;
import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.SettlementCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MerchantServiceTest {
    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    private ChangeBankAccountRequest changeBankAccountRequest;
    private Merchant merchant;

    @BeforeEach
    public void setUp() {
        String businessNumber = "12345-65434";
        String bankName = "SHINHAN";
        String changedBankName = "KB";
        String accountNumber = "123-456-789";
        String accountHolder = "hyuk";

        changeBankAccountRequest = ChangeBankAccountRequest.builder()
                .bankAccount(new BankAccount(changedBankName, accountNumber, accountHolder))
                .businessNumber(businessNumber)
                .build();

        merchant = Merchant.create(
                businessNumber,
                "cafe",
                new BankAccount(bankName, accountNumber, accountHolder),
                SettlementCycle.D_PLUS_1
        );
    }

    @Test
    void 계좌_변경_성공() {
        when(merchantRepository.findByBusinessNumber(any())).thenReturn(Optional.ofNullable(merchant));

        merchantService.changeBankAccount(changeBankAccountRequest);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);

        verify(merchantRepository, times(1)).findByBusinessNumber(any());
        verify(merchantRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getBankAccount().getBankName()).isEqualTo("KB");
    }
}
