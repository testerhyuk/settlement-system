package com.hyuk.settlement.infrastructure.ledger;

import com.hyuk.settlement.ledger.Account;
import com.hyuk.settlement.ledger.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Account save(Account account) {
        AccountEntity accountEntity = AccountEntity.from(account);
        accountJpaRepository.save(accountEntity);
        return accountEntity.toDomain();
    }

    @Override
    public Optional<Account> findById(String id) {
        return accountJpaRepository.findById(id).map(AccountEntity::toDomain);
    }

    @Override
    public List<Account> findByOwnerId(String ownerId) {
        return accountJpaRepository.findByOwnerId(ownerId).stream().map(AccountEntity::toDomain).toList();
    }
}
