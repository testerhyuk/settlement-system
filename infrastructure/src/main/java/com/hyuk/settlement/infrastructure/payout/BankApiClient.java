package com.hyuk.settlement.infrastructure.payout;

import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.Money;
import org.springframework.stereotype.Component;

@Component
public class BankApiClient {
    public boolean transfer(BankAccount bankAccount, Money amount) {
        return Math.random() > 0.2;
    }
}
