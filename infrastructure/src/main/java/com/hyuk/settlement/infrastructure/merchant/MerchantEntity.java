package com.hyuk.settlement.infrastructure.merchant;

import com.hyuk.settlement.merchant.Merchant;
import com.hyuk.settlement.shared.BankAccount;
import com.hyuk.settlement.shared.SettlementCycle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "merchant")
public class MerchantEntity {
    @Id
    @Column(unique = true, nullable = false)
    private String merchantId;
    @Column(unique = true, nullable = false)
    private String businessNumber;
    private String name;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    @Enumerated(EnumType.STRING)
    private SettlementCycle settlementCycle;

    public static MerchantEntity from(Merchant merchant) {
        MerchantEntity merchantEntity = new MerchantEntity();
        merchantEntity.merchantId = merchant.getMerchantId();
        merchantEntity.bankName = merchant.getBankAccount().getBankName();
        merchantEntity.accountNumber = merchant.getBankAccount().getAccountNumber();
        merchantEntity.accountHolder = merchant.getBankAccount().getAccountHolder();
        merchantEntity.businessNumber = merchant.getBusinessNumber();
        merchantEntity.name = merchant.getName();
        merchantEntity.settlementCycle = merchant.getSettlementCycle();

        return merchantEntity;
    }

    public Merchant toDomain() {
        return new Merchant(
                merchantId,
                businessNumber,
                name,
                new BankAccount(bankName, accountNumber, accountHolder),
                settlementCycle
        );
    }
}
