package com.hyuk.settlement.api.merchant;

import com.hyuk.settlement.shared.BankAccount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangeBankAccountRequest {
    private String businessNumber;
    private BankAccount bankAccount;
}
