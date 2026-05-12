package com.hyuk.settlement.merchant;

import java.util.Optional;

public interface MerchantRepository {
    Merchant save(Merchant merchant);
    Optional<Merchant> findById(String id);
    Optional<Merchant> findByBusinessNumber(String businessNumber);
}
