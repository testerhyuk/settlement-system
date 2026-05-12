package com.hyuk.settlement.ledger;

import com.hyuk.settlement.ledger.enums.AccountType;
import lombok.Getter;

@Getter
public class Account {
    private String accountId;
    private AccountType accountType;
    private String name;
    private String ownerId;

    public Account(String accountId, AccountType accountType, String name, String ownerId) {
        if (accountId == null || accountId.isBlank()) throw new IllegalArgumentException("accountId는 필수입니다");
        if (accountType == null) throw new IllegalArgumentException("계정 유형은 필수입니다");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("계정 이름은 필수입니다");
        if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("ownerId는 필수입니다");

        this.accountId = accountId;
        this.accountType = accountType;
        this.name = name;
        this.ownerId = ownerId;
    }
}
