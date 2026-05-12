package com.hyuk.settlement.merchant;

import lombok.Getter;
import com.hyuk.settlement.shared.SettlementCycle;
import com.hyuk.settlement.shared.BankAccount;

import java.util.UUID;

@Getter
public class Merchant {
    private String merchantId;
    private String businessNumber;
    private String name;
    private BankAccount bankAccount;
    private SettlementCycle settlementCycle;

    public Merchant(String merchantId, String businessNumber, String name, BankAccount bankAccount, SettlementCycle settlementCycle) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("Merchant ID는 필수입니다");
        }

        if (businessNumber == null || businessNumber.isBlank()) {
            throw new IllegalArgumentException("사업자등록번호는 필수입니다");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("가맹점은 필수입니다");
        }

        if (settlementCycle == null) {
            throw new IllegalArgumentException("정산 주기는 필수입니다");
        }

        if (bankAccount == null) {
            throw new IllegalArgumentException("계좌번호는 필수입니다");
        }

        this.merchantId = merchantId;
        this.businessNumber = businessNumber;
        this.name = name;
        this.bankAccount = bankAccount;
        this.settlementCycle = settlementCycle;
    }

    public static Merchant create(String businessNumber, String name, BankAccount bankAccount, SettlementCycle settlementCycle) {
        return new Merchant(
                "merchant-" + UUID.randomUUID().toString(),
                businessNumber,
                name,
                bankAccount,
                settlementCycle
        );
    }
}
