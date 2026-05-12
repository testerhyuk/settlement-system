package com.hyuk.settlement.shared;

import lombok.Getter;

@Getter
public class BankAccount {
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    public BankAccount(String bankName, String accountNumber, String accountHolder) {
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("은행명은 필수입니다");
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다");
        }

        if (accountHolder == null || accountHolder.isBlank()) {
            throw new IllegalArgumentException("예금주는 필수입니다");
        }

        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}
