package com.hyuk.settlement.api.merchant;

import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.SettlementCycle;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterMerchantRequest {
    private String businessNumber;
    private String name;
    private BankAccount bankAccount;
    private SettlementCycle settlementCycle;
}
