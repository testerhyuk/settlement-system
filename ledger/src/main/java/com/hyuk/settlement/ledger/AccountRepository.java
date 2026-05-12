package com.hyuk.settlement.ledger;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(String id);
    List<Account> findByOwnerId(String ownerId);
}
