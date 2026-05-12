package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.Account;
import com.hyuk.settlement.ledger.enums.AccountType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account")
public class AccountEntity {
    @Id
    private String accountId;
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    private String name;
    private String ownerId;

    public static AccountEntity from(Account account) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.accountId = account.getAccountId();
        accountEntity.accountType = account.getAccountType();
        accountEntity.name = account.getName();
        accountEntity.ownerId = account.getOwnerId();
        return accountEntity;
    }

    public Account toDomain() {
        return new Account(accountId, accountType, name, ownerId);
    }
}
